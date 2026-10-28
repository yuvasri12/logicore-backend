package com.devbridge.logicore.repository;

import com.devbridge.logicore.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findAllByOrderByNameAsc();

    List<Warehouse> findByRegionIdOrderByNameAsc(Long regionId);

    Optional<Warehouse> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}