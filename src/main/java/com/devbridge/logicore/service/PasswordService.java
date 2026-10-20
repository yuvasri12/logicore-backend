package com.devbridge.logicore.service;

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

@Service
public class PasswordService {

    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);
    private static final String ENTITY = "USER";

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public PasswordService(AppUserRepository users, PasswordEncoder passwordEncoder, AuditService audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    /** A logged-in user changes their own password. */
    @Transactional
    public void changeOwn(Long userId, String currentPassword, String newPassword) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_UNCHANGED);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        audit.record(AuditAction.PASSWORD_CHANGED, ENTITY, user.getId(), "email=" + user.getEmail());
        log.info("Password changed: userId={}", user.getId());
    }

    /** An admin sets a new password for another user. Never logs or audits the password itself. */
    @Transactional
    public void reset(Long targetId, String newPassword, Long actorId) {
        AppUser user = users.findById(targetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getId().equals(actorId)) {
            throw new BusinessException(ErrorCode.USER_SELF_CHANGE_NOT_ALLOWED);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        audit.record(AuditAction.PASSWORD_RESET, ENTITY, user.getId(), "email=" + user.getEmail());
        log.info("Password reset: userId={}, byUserId={}", user.getId(), actorId);
    }
}
