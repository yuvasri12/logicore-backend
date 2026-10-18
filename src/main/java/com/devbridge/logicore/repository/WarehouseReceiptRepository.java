package com.devbridge.logicore.repository;

import com.devbridge.logicore.model.WarehouseReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseReceiptRepository
        extends JpaRepository<WarehouseReceipt, Long> {
}