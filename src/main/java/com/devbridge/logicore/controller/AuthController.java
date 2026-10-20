package com.devbridge.logicore.controller;

import com.devbridge.logicore.dto.ChangePasswordRequest;
import com.devbridge.logicore.dto.LoginRequest;
import com.devbridge.logicore.dto.LoginResponse;
import com.devbridge.logicore.dto.UserResponse;
import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.exception.BusinessException;
import com.devbridge.logicore.security.AuthenticatedUser;
import com.devbridge.logicore.security.CurrentUser;
import com.devbridge.logicore.service.AuthService;
import com.devbridge.logicore.service.PasswordService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordService passwordService;

    public AuthController(AuthService authService, PasswordService passwordService) {
        this.authService = authService;
        this.passwordService = passwordService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me() {
        AuthenticatedUser user = CurrentUser.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        AuthenticatedUser user = CurrentUser.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        passwordService.changeOwn(user.getId(), request.getCurrentPassword(), request.getNewPassword());
    }
}
