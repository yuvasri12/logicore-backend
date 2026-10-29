package com.devbridge.logicore.service;

import com.devbridge.logicore.enums.Role;
import com.devbridge.logicore.model.AppUser;
import com.devbridge.logicore.repository.AppUserRepository;
import com.devbridge.logicore.security.AuthenticatedUser;
import com.devbridge.logicore.security.CurrentUser;
import com.devbridge.logicore.security.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Works out which data the logged-in user may see.
 * Reads the assignment from the database each time, so a change by Admin applies at once.
 */
@Service
public class ScopeService {

    private final AppUserRepository users;

    public ScopeService(AppUserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Scope current() {
        AuthenticatedUser me = CurrentUser.get().orElse(null);
        if (me == null) {
            return Scope.none();
        }
        if (me.getRole() == Role.ADMIN) {
            return Scope.all();
        }

        AppUser user = users.findById(me.getId()).orElse(null);
        if (user == null || !user.isEnabled()) {
            return Scope.none();
        }

        if (user.getRole() == Role.OPS_MANAGER) {
            if (user.getRegion() != null) {
                return Scope.region(user.getRegion().getId());
            }
            if (user.getWarehouse() != null && user.getWarehouse().getRegion() != null) {
                return Scope.region(user.getWarehouse().getRegion().getId());
            }
            return Scope.none();
        }

        if (user.getWarehouse() != null) {
            return Scope.warehouse(user.getWarehouse().getId());
        }
        return Scope.none();
    }
}