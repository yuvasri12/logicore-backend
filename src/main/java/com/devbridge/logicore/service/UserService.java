package com.devbridge.logicore.service;

import com.devbridge.logicore.dto.CreateUserRequest;
import com.devbridge.logicore.dto.UpdateUserRequest;
import com.devbridge.logicore.dto.UserAdminResponse;
import com.devbridge.logicore.enums.AuditAction;
import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.enums.Role;
import com.devbridge.logicore.exception.BusinessException;
import com.devbridge.logicore.exception.ResourceNotFoundException;
import com.devbridge.logicore.model.AppUser;
import com.devbridge.logicore.model.Region;
import com.devbridge.logicore.model.Warehouse;
import com.devbridge.logicore.repository.AppUserRepository;
import com.devbridge.logicore.repository.RegionRepository;
import com.devbridge.logicore.repository.WarehouseRepository;
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
    private final RegionRepository regions;
    private final WarehouseRepository warehouses;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserService(AppUserRepository users, RegionRepository regions, WarehouseRepository warehouses,
                       PasswordEncoder passwordEncoder, AuditService audit) {
        this.users = users;
        this.regions = regions;
        this.warehouses = warehouses;
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
        applyScope(user, request.getRegionId(), request.getWarehouseId(), true);

        AppUser saved = users.save(user);
        audit.record(AuditAction.USER_CREATED, ENTITY, saved.getId(),
                "email=" + saved.getEmail() + ", role=" + saved.getRole() + scopeText(saved));
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
        boolean scopeChanged = applyScope(user, request.getRegionId(), request.getWarehouseId(), roleChanged);
        AppUser saved = users.save(user);

        if (roleChanged) {
            audit.record(AuditAction.USER_UPDATED, ENTITY, saved.getId(),
                    "email=" + saved.getEmail() + ", role=" + oldRole + " -> " + saved.getRole());
        }
        if (scopeChanged) {
            audit.record(AuditAction.USER_ASSIGNED, ENTITY, saved.getId(),
                    "email=" + saved.getEmail() + scopeText(saved));
        }
        if (statusChanged) {
            audit.record(saved.isEnabled() ? AuditAction.USER_ENABLED : AuditAction.USER_DISABLED,
                    ENTITY, saved.getId(), "email=" + saved.getEmail());
        }
        log.info("User updated: id={}, role={}, enabled={}", saved.getId(), saved.getRole(), saved.isEnabled());
        return toResponse(saved);
    }

    /**
     * Sets where the user works, using the user's role as it is now.
     * Admin has no assignment. Ops Manager needs a region. Warehouse Staff and Dispatcher need a warehouse.
     * When no ids are sent, the current assignment is kept, unless it must be complete
     * (a new user, or a role change), in which case a missing assignment is refused.
     * Returns true when the assignment changed.
     */
    private boolean applyScope(AppUser user, Long regionId, Long warehouseId, boolean mustBeComplete) {
        Long oldRegion = user.getRegion() == null ? null : user.getRegion().getId();
        Long oldWarehouse = user.getWarehouse() == null ? null : user.getWarehouse().getId();
        Role role = user.getRole();

        if (role == Role.ADMIN) {
            user.setRegion(null);
            user.setWarehouse(null);
            return oldRegion != null || oldWarehouse != null;
        }

        boolean needsRegion = role == Role.OPS_MANAGER;

        if (regionId == null && warehouseId == null) {
            boolean complete = needsRegion ? user.getRegion() != null : user.getWarehouse() != null;
            if (mustBeComplete && !complete) {
                throw new BusinessException(ErrorCode.ASSIGNMENT_REQUIRED);
            }
            if (!complete) {
                return false;
            }
            // Keep only the part that fits the role (for example after a role change).
            if (needsRegion) {
                user.setWarehouse(null);
            } else {
                user.setRegion(null);
            }
            return changed(user, oldRegion, oldWarehouse);
        }

        if (needsRegion) {
            if (regionId == null) {
                throw new BusinessException(ErrorCode.ASSIGNMENT_REQUIRED);
            }
            user.setRegion(findRegion(regionId));
            user.setWarehouse(null);
        } else {
            if (warehouseId == null) {
                throw new BusinessException(ErrorCode.ASSIGNMENT_REQUIRED);
            }
            user.setWarehouse(findWarehouse(warehouseId));
            user.setRegion(null);
        }
        return changed(user, oldRegion, oldWarehouse);
    }

    private boolean changed(AppUser user, Long oldRegion, Long oldWarehouse) {
        Long newRegion = user.getRegion() == null ? null : user.getRegion().getId();
        Long newWarehouse = user.getWarehouse() == null ? null : user.getWarehouse().getId();
        return !java.util.Objects.equals(oldRegion, newRegion)
                || !java.util.Objects.equals(oldWarehouse, newWarehouse);
    }

    private Region findRegion(Long id) {
        return regions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.REGION_NOT_FOUND, id));
    }

    private Warehouse findWarehouse(Long id) {
        return warehouses.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.WAREHOUSE_NOT_FOUND, id));
    }

    private String scopeText(AppUser user) {
        StringBuilder text = new StringBuilder();
        if (user.getRegion() != null) {
            text.append(", region=").append(user.getRegion().getCode());
        }
        if (user.getWarehouse() != null) {
            text.append(", warehouse=").append(user.getWarehouse().getCode());
        }
        return text.toString();
    }

    private UserAdminResponse toResponse(AppUser user) {
        Region region = user.getRegion();
        Warehouse warehouse = user.getWarehouse();
        return new UserAdminResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole(), user.isEnabled(), user.getCreatedAt(),
                region == null ? null : region.getId(), region == null ? null : region.getName(),
                warehouse == null ? null : warehouse.getId(), warehouse == null ? null : warehouse.getName());
    }
}