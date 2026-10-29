package com.devbridge.logicore.repository;

import com.devbridge.logicore.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    /** Shipments that start or end in any warehouse of the region. */
    @Query("select distinct s from Shipment s "
            + "left join s.originWarehouse ow left join s.destinationWarehouse dw "
            + "where ow.region.id = :regionId or dw.region.id = :regionId "
            + "order by s.createdAt desc")
    List<Shipment> findVisibleToRegion(@Param("regionId") Long regionId);

    /** Shipments that start or end at the warehouse. */
    @Query("select distinct s from Shipment s "
            + "left join s.originWarehouse ow left join s.destinationWarehouse dw "
            + "where ow.id = :warehouseId or dw.id = :warehouseId "
            + "order by s.createdAt desc")
    List<Shipment> findVisibleToWarehouse(@Param("warehouseId") Long warehouseId);
}