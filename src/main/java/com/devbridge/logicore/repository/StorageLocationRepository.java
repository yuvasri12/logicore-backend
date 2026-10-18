package com.devbridge.logicore.repository;

import com.devbridge.logicore.model.StorageLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {

    Optional<StorageLocation> findByZoneAndRackAndBin(String zone, String rack, String bin);
}