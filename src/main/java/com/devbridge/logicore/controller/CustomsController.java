package com.devbridge.logicore.controller;

import com.devbridge.logicore.dto.ClearCustomsRequest;
import com.devbridge.logicore.service.CustomsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/customs")
public class CustomsController {

    private final CustomsService customsService;

    public CustomsController(CustomsService customsService) {
        this.customsService = customsService;
    }

    /** Ops Manager or Admin records that customs has cleared an international shipment. */
    @PostMapping("/shipments/{shipmentId}/clear")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@PathVariable Long shipmentId, @Valid @RequestBody ClearCustomsRequest request) {
        customsService.clear(shipmentId, request.getCustomsReference(), request.getNote());
    }
}