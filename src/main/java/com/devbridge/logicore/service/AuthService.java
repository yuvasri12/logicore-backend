package com.devbridge.logicore.service;

import com.devbridge.logicore.dto.LoginRequest;
import com.devbridge.logicore.dto.LoginResponse;
import com.devbridge.logicore.dto.UserResponse;
import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.exception.BusinessException;
import com.devbridge.logicore.model.AppUser;
import com.devbridge.logicore.repository.AppUserRepository;
import com.devbridge.logicore.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder,
                       JwtService jwtService, LoginAttemptService loginAttempts) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim();

        // Stop early if this email is locked (throws LOGIN_LOCKED).
        loginAttempts.assertNotLocked(email);

        AppUser user = users.findByEmailIgnoreCase(email).orElse(null);

        if (user == null || !user.isEnabled()
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttempts.recordFailure(email);
            log.warn("Login failed");
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }

        loginAttempts.recordSuccess(email);

        String token = jwtService.generate(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
        log.info("Login ok: userId={}, role={}", user.getId(), user.getRole());

        return new LoginResponse(token, jwtService.getExpiresInSeconds(),
                new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole()));
    }
}