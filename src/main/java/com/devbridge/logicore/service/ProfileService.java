package com.devbridge.logicore.service;

import com.devbridge.logicore.dto.ProfileResponse;
import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.exception.BusinessException;
import com.devbridge.logicore.model.AppUser;
import com.devbridge.logicore.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final AppUserRepository userRepository;

    public ProfileService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Loads the user fresh from the database, so a changed role or location shows up at once. */
    @Transactional(readOnly = true)
    public ProfileResponse get(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        String regionName = user.getRegion() == null ? null : user.getRegion().getName();
        String warehouseName = user.getWarehouse() == null ? null : user.getWarehouse().getName();
        return new ProfileResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                regionName, warehouseName);
    }
}