package com.devbridge.logicore.repository;

import com.devbridge.logicore.model.CustomsClearance;
import com.devbridge.logicore.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomsClearanceRepository extends JpaRepository<CustomsClearance, Long> {

    boolean existsByShipment(Shipment shipment);
}