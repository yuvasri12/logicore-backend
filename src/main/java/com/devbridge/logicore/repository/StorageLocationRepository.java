package com.devbridge.logicore.repository;

import com.devbridge.logicore.model.StorageLocation;
import com.devbridge.logicore.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {

    Optional<StorageLocation> findByWarehouseAndZoneAndRackAndBin(
            Warehouse warehouse, String zone, String rack, String bin);

    /** Old rows that were created before locations belonged to a warehouse. */
    Optional<StorageLocation> findByWarehouseIsNullAndZoneAndRackAndBin(
            String zone, String rack, String bin);
}