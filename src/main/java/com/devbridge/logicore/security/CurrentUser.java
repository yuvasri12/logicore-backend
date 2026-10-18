package com.devbridge.logicore.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AuthenticatedUser> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser) {
            return Optional.of((AuthenticatedUser) auth.getPrincipal());
        }
        return Optional.empty();
    }

    /** The logged-in user's name; if nobody is logged in, the fallback, or SYSTEM. */
    public static String nameOr(String fallback) {
        Optional<AuthenticatedUser> user = get();
        if (user.isPresent()) {
            return user.get().getFullName();
        }
        return fallback != null ? fallback : "SYSTEM";
    }
}