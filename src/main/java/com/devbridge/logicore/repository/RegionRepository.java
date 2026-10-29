package com.devbridge.logicore.repository;

import com.devbridge.logicore.model.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByOrderByNameAsc();

    Optional<Region> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}