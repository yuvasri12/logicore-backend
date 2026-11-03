package com.devbridge.logicore.repository;

import com.devbridge.logicore.model.WarehouseReceipt;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** Reads the receipts of many shipments in one query. */
public interface WarehouseReceiptLookupRepository extends Repository<WarehouseReceipt, Long> {

    /** Newest receipt first, so the first one seen for a shipment is its latest. */
    @Query("select r from WarehouseReceipt r where r.shipment.id in :ids "
            + "order by r.receivedAt desc, r.id desc")
    List<WarehouseReceipt> findForShipments(@Param("ids") Collection<Long> ids);
}