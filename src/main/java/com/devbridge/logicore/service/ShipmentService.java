package com.devbridge.logicore.service;

import com.devbridge.logicore.dto.*;
import com.devbridge.logicore.enums.ShipmentMode;
import com.devbridge.logicore.enums.ShipmentStatus;

import java.util.List;

public interface ShipmentService {

    ShipmentResponse createShipment(CreateShipmentRequest request);

    List<ShipmentResponse> listShipments(
            ShipmentStatus status,
            ShipmentMode mode,
            String customerName);

    /** One page of the shipments the user may see. Filters, search and sorting run in the database. */
    PagedResponse<ShipmentResponse> searchShipments(
            ShipmentStatus status,
            ShipmentMode mode,
            String text,
            int page,
            int size,
            String sortBy,
            String dir);

    ShipmentResponse getShipment(Long id);

    ShipmentResponse updateShipment(
            Long id,
            UpdateShipmentRequest request);

    ShipmentResponse updateStatus(
            Long id,
            UpdateStatusRequest request);

    ShipmentResponse markLoaded(
            Long id,
            ShipmentActionRequest request);

    ShipmentResponse markDelivered(
            Long id,
            ShipmentActionRequest request);

    ShipmentResponse cancelShipment(
            Long id,
            CancelShipmentRequest request);

    TrackingResponse getTracking(String trackingNumber);
}