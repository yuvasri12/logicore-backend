package com.devbridge.logicore.config;

import com.devbridge.logicore.enums.Role;
import com.devbridge.logicore.model.AppUser;
import com.devbridge.logicore.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
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
            createIfMissing("ops.manager@logicore.local", "Ops Manager", Role.OPS_MANAGER);
            createIfMissing("warehouse@logicore.local", "Warehouse Staff", Role.WAREHOUSE_STAFF);
            createIfMissing("dispatcher@logicore.local", "Dispatcher", Role.DISPATCHER);
        }
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