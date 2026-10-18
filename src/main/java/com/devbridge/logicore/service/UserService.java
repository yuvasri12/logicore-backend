package com.devbridge.logicore.service;

import com.devbridge.logicore.dto.CreateUserRequest;
import com.devbridge.logicore.dto.UpdateUserRequest;
import com.devbridge.logicore.dto.UserAdminResponse;
import com.devbridge.logicore.enums.AuditAction;
import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.exception.BusinessException;
import com.devbridge.logicore.model.AppUser;
import com.devbridge.logicore.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String ENTITY = "USER";

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserService(AppUserRepository users, PasswordEncoder passwordEncoder, AuditService audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<UserAdminResponse> list() {
        List<UserAdminResponse> result = new ArrayList<UserAdminResponse>();
        for (AppUser user : users.findAllByOrderByFullNameAsc()) {
            result.add(toResponse(user));
        }
        return result;
    }

    @Transactional
    public UserAdminResponse create(CreateUserRequest request) {
        String email = request.getEmail().trim();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setFullName(request.getFullName().trim());
        user.setRole(request.getRole());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);

        AppUser saved = users.save(user);
        audit.record(AuditAction.USER_CREATED, ENTITY, saved.getId(),
                "email=" + saved.getEmail() + ", role=" + saved.getRole());
        log.info("User created: id={}, role={}", saved.getId(), saved.getRole());
        return toResponse(saved);
    }

    @Transactional
    public UserAdminResponse update(Long id, UpdateUserRequest request, Long actorId) {
        AppUser user = users.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean roleChanged = user.getRole() != request.getRole();
        boolean statusChanged = user.isEnabled() != request.getEnabled().booleanValue();
        if (user.getId().equals(actorId) && (roleChanged || statusChanged)) {
            throw new BusinessException(ErrorCode.USER_SELF_CHANGE_NOT_ALLOWED);
        }

        Object oldRole = user.getRole();
        user.setRole(request.getRole());
        user.setEnabled(request.getEnabled().booleanValue());
        AppUser saved = users.save(user);

        if (roleChanged) {
            audit.record(AuditAction.USER_UPDATED, ENTITY, saved.getId(),
                    "email=" + saved.getEmail() + ", role=" + oldRole + " -> " + saved.getRole());
        }
        if (statusChanged) {
            audit.record(saved.isEnabled() ? AuditAction.USER_ENABLED : AuditAction.USER_DISABLED,
                    ENTITY, saved.getId(), "email=" + saved.getEmail());
        }
        log.info("User updated: id={}, role={}, enabled={}", saved.getId(), saved.getRole(), saved.isEnabled());
        return toResponse(saved);
    }

    private UserAdminResponse toResponse(AppUser user) {
        return new UserAdminResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole(), user.isEnabled(), user.getCreatedAt());
    }
}