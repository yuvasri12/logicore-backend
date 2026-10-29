package com.devbridge.logicore.config;

import com.devbridge.logicore.enums.Role;
import com.devbridge.logicore.model.AppUser;
import com.devbridge.logicore.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Order 1: users are created first, then OrganizationBootstrap (order 2) assigns them to locations.
@Component
@Order(1)
public class UserBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserBootstrap.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String adminEmail;
    private final String adminName;
    private final String password;
    private final boolean demoUsers;

    public UserBootstrap(AppUserRepository users,
                         PasswordEncoder encoder,
                         @Value("${app.bootstrap.admin.email:}") String adminEmail,
                         @Value("${app.bootstrap.admin.name:Admin}") String adminName,
                         @Value("${app.bootstrap.admin.password:}") String password,
                         @Value("${app.bootstrap.demo-users:false}") boolean demoUsers) {
        this.users = users;
        this.encoder = encoder;
        this.adminEmail = adminEmail;
        this.adminName = adminName;
        this.password = password;
        this.demoUsers = demoUsers;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (isBlank(password)) {
            log.warn("app.bootstrap.admin.password is empty, so no users are created.");
            return;
        }
        if (users.count() == 0) {
            if (isBlank(adminEmail)) {
                log.warn("No users exist and app.bootstrap.admin.email is empty, so nobody can log in.");
            } else {
                createIfMissing(adminEmail, adminName, Role.ADMIN);
            }
        }
        if (demoUsers) {
            // One-time move from the first demo logins to named accounts. It keeps the password and
            // the region / warehouse assignment. Safe to leave in; it does nothing once renamed.
            rename("ops.manager@logicore.local", "priya.nair@logicore.com", "Priya Nair");
            rename("warehouse@logicore.local", "karthik.raja@logicore.com", "Karthik Raja");
            rename("dispatcher@logicore.local", "meena.s@logicore.com", "Meena Subramanian");
            rename("ops.uae@logicore.local", "omar.haddad@logicore.com", "Omar Haddad");
            rename("warehouse.dxb@logicore.local", "faisal.khan@logicore.com", "Faisal Khan");

            createIfMissing("priya.nair@logicore.com", "Priya Nair", Role.OPS_MANAGER);
            createIfMissing("karthik.raja@logicore.com", "Karthik Raja", Role.WAREHOUSE_STAFF);
            createIfMissing("meena.s@logicore.com", "Meena Subramanian", Role.DISPATCHER);
            // A second region, to show that each person only sees their own area.
            createIfMissing("omar.haddad@logicore.com", "Omar Haddad", Role.OPS_MANAGER);
            createIfMissing("faisal.khan@logicore.com", "Faisal Khan", Role.WAREHOUSE_STAFF);
        }
    }

    /** Renames an old demo user in place, unless the new email is already taken. */
    private void rename(String oldEmail, String newEmail, String newName) {
        if (users.existsByEmailIgnoreCase(newEmail)) {
            return;
        }
        AppUser user = users.findByEmailIgnoreCase(oldEmail).orElse(null);
        if (user == null) {
            return;
        }
        user.setEmail(newEmail.trim().toLowerCase());
        user.setFullName(newName);
        users.save(user);
        log.info("Demo user renamed: id={}", user.getId());
    }

    private void createIfMissing(String email, String name, Role role) {
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        AppUser user = new AppUser();
        user.setEmail(email.trim().toLowerCase());
        user.setFullName(name);
        user.setPasswordHash(encoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        AppUser saved = users.save(user);
        log.info("User created: id={}, role={}", saved.getId(), role);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}