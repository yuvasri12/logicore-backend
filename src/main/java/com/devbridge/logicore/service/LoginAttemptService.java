package com.devbridge.logicore.service;

import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts wrong passwords per email and locks the account for a while.
 * Kept in memory: it resets when the server restarts, and each server instance counts on its own.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private static class Attempt {
        private final int failures;
        private final long lockedUntilMillis;

        private Attempt(int failures, long lockedUntilMillis) {
            this.failures = failures;
            this.lockedUntilMillis = lockedUntilMillis;
        }
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<String, Attempt>();
    private final int maxAttempts;
    private final long lockMillis;

    public LoginAttemptService(
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.lock-minutes:10}") int lockMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockMillis = lockMinutes * 60L * 1000L;
    }

    /** Call before checking the password. Throws LOGIN_LOCKED while the email is locked. */
    public void assertNotLocked(String email) {
        Attempt attempt = attempts.get(key(email));
        if (attempt == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (attempt.lockedUntilMillis > now) {
            long minutesLeft = (attempt.lockedUntilMillis - now + 59999L) / 60000L;
            log.warn("Login blocked: account is locked, minutesLeft={}", minutesLeft);
            throw new BusinessException(ErrorCode.LOGIN_LOCKED, minutesLeft);
        }
    }

    /** Call after a wrong password. */
    public void recordFailure(final String email) {
        final long now = System.currentTimeMillis();
        attempts.compute(key(email), (k, old) -> {
            int failures = 1;
            if (old != null && !(old.lockedUntilMillis != 0 && old.lockedUntilMillis <= now)) {
                failures = old.failures + 1;
            }
            if (failures >= maxAttempts) {
                log.warn("Too many failed logins: locking account for {} ms", lockMillis);
                return new Attempt(0, now + lockMillis);
            }
            return new Attempt(failures, 0L);
        });
    }

    /** Call after a successful login. */
    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}