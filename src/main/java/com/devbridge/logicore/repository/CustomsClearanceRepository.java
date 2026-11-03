package com.devbridge.logicore.repository;

import com.devbridge.logicore.model.CustomsClearance;
import com.devbridge.logicore.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CustomsClearanceRepository extends JpaRepository<CustomsClearance, Long> {

    boolean existsByShipment(Shipment shipment);

    /** Which of these shipments already have a customs clearance, in one query. */
    @Query("select c.shipment.id from CustomsClearance c where c.shipment.id in :ids")
    List<Long> findClearedShipmentIds(@Param("ids") Collection<Long> ids);
}