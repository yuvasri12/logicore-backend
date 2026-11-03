package com.devbridge.logicore.repository;

import com.devbridge.logicore.model.Shipment;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

/** Paged, filtered shipment search. The filters are built in ShipmentSpecs. */
public interface ShipmentSearchRepository
        extends Repository<Shipment, Long>, JpaSpecificationExecutor<Shipment> {
}