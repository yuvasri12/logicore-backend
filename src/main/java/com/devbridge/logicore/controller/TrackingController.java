package com.devbridge.logicore.controller;

import com.devbridge.logicore.dto.TrackingResponse;
import com.devbridge.logicore.service.ShipmentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private final ShipmentService shipmentService;

    public TrackingController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping("/{trackingNumber}")
    public TrackingResponse getTracking(@PathVariable String trackingNumber) {
        return shipmentService.getTracking(trackingNumber);
    }
}
