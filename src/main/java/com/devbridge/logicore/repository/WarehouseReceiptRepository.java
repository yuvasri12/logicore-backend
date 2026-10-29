package com.devbridge.logicore.repository;

import com.devbridge.logicore.model.Shipment;
import com.devbridge.logicore.model.WarehouseReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarehouseReceiptRepository
        extends JpaRepository<WarehouseReceipt, Long> {

    /** The most recent receipt of a shipment. */
    Optional<WarehouseReceipt> findFirstByShipmentOrderByReceivedAtDescIdDesc(Shipment shipment);
}