package com.devbridge.logicore.service.impl;

import com.devbridge.logicore.dto.CancelShipmentRequest;
import com.devbridge.logicore.dto.CreateShipmentRequest;
import com.devbridge.logicore.dto.ShipmentActionRequest;
import com.devbridge.logicore.dto.ShipmentResponse;
import com.devbridge.logicore.dto.StatusHistoryResponse;
import com.devbridge.logicore.dto.TrackingResponse;
import com.devbridge.logicore.dto.TrackingTimelineResponse;
import com.devbridge.logicore.dto.UpdateShipmentRequest;
import com.devbridge.logicore.dto.UpdateStatusRequest;
import com.devbridge.logicore.enums.AuditAction;
import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.enums.ShipmentMode;
import com.devbridge.logicore.enums.ShipmentStatus;
import com.devbridge.logicore.exception.BusinessException;
import com.devbridge.logicore.exception.ResourceNotFoundException;
import com.devbridge.logicore.message.MessageService;
import com.devbridge.logicore.model.Customer;
import com.devbridge.logicore.model.Shipment;
import com.devbridge.logicore.model.ShipmentStatusHistory;
import com.devbridge.logicore.repository.CustomerRepository;
import com.devbridge.logicore.repository.ShipmentRepository;
import com.devbridge.logicore.security.CurrentUser;
import com.devbridge.logicore.service.AuditService;
import com.devbridge.logicore.service.ShipmentService;
import com.devbridge.logicore.service.ShipmentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ShipmentServiceImpl implements ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentServiceImpl.class);
    private static final String ENTITY = "SHIPMENT";

    private final ShipmentRepository shipmentRepository;
    private final CustomerRepository customerRepository;
    private final MessageService messages;
    private final AuditService audit;

    public ShipmentServiceImpl(ShipmentRepository shipmentRepository,
                               CustomerRepository customerRepository,
                               MessageService messages,
                               AuditService audit) {
        this.shipmentRepository = shipmentRepository;
        this.customerRepository = customerRepository;
        this.messages = messages;
        this.audit = audit;
    }

    @Override
    public ShipmentResponse createShipment(CreateShipmentRequest request) {

        Shipment shipment = new Shipment();

        applyCustomer(shipment, request.getCustomerId(), request.getCustomerName());

        shipment.setTrackingNumber("TRK-" + System.currentTimeMillis());
        shipment.setOrigin(request.getOrigin());
        shipment.setDestination(request.getDestination());
        shipment.setMode(request.getMode());
        shipment.setWeight(request.getWeight());
        shipment.setStatus(ShipmentStatus.NEW);

        shipment.addStatusHistory(buildHistory(ShipmentStatus.NEW, "Shipment created", "SYSTEM"));

        Shipment savedShipment = shipmentRepository.save(shipment);

        audit.record(AuditAction.SHIPMENT_CREATED, ENTITY, savedShipment.getId(),
                "tracking=" + savedShipment.getTrackingNumber()
                        + ", customer=" + savedShipment.getCustomerName());
        log.info("Shipment created: id={}, tracking={}",
                savedShipment.getId(), savedShipment.getTrackingNumber());

        return mapToResponse(savedShipment);
    }

    @Override
    public List<ShipmentResponse> listShipments(
            ShipmentStatus status,
            ShipmentMode mode,
            String customerName) {

        List<Shipment> shipments = shipmentRepository.findAll();
        List<ShipmentResponse> responses = new ArrayList<ShipmentResponse>();

        for (Shipment shipment : shipments) {
            if (status != null && shipment.getStatus() != status) {
                continue;
            }
            if (mode != null && shipment.getMode() != mode) {
                continue;
            }
            if (customerName != null && !customerName.trim().isEmpty()
                    && (shipment.getCustomerName() == null
                    || !shipment.getCustomerName().toLowerCase().contains(customerName.toLowerCase()))) {
                continue;
            }
            responses.add(mapToResponse(shipment));
        }

        return responses;
    }

    @Override
    public ShipmentResponse getShipment(Long id) {
        Shipment shipment = findShipmentOrThrow(id);
        return mapToResponse(shipment);
    }

    @Override
    public ShipmentResponse updateShipment(Long id, UpdateShipmentRequest request) {

        Shipment shipment = findShipmentOrThrow(id);

        applyCustomer(shipment, request.getCustomerId(), request.getCustomerName());

        shipment.setOrigin(request.getOrigin());
        shipment.setDestination(request.getDestination());
        shipment.setMode(request.getMode());
        shipment.setWeight(request.getWeight());

        Shipment updatedShipment = shipmentRepository.save(shipment);

        audit.record(AuditAction.SHIPMENT_UPDATED, ENTITY, updatedShipment.getId(),
                "tracking=" + updatedShipment.getTrackingNumber()
                        + ", customer=" + updatedShipment.getCustomerName());

        return mapToResponse(updatedShipment);
    }

    @Override
    public ShipmentResponse updateStatus(Long id, UpdateStatusRequest request) {
        Shipment shipment = findShipmentOrThrow(id);
        return changeStatus(shipment, request.getStatus(), request.getRemarks(), request.getUpdatedBy());
    }

    @Override
    public ShipmentResponse markLoaded(Long id, ShipmentActionRequest request) {
        Shipment shipment = findShipmentOrThrow(id);
        String remarks = hasText(request.getRemarks()) ? request.getRemarks() : "Loaded and ready to leave";
        return changeStatus(shipment, ShipmentStatus.LOADED, remarks, request.getUpdatedBy());
    }

    @Override
    public ShipmentResponse markDelivered(Long id, ShipmentActionRequest request) {
        Shipment shipment = findShipmentOrThrow(id);
        String remarks = hasText(request.getRemarks()) ? request.getRemarks() : "Delivered";
        return changeStatus(shipment, ShipmentStatus.DELIVERED, remarks, request.getUpdatedBy());
    }

    @Override
    public ShipmentResponse cancelShipment(Long id, CancelShipmentRequest request) {
        Shipment shipment = findShipmentOrThrow(id);
        return changeStatus(shipment, ShipmentStatus.CANCELLED, request.getReason(), request.getCancelledBy());
    }

    @Override
    public TrackingResponse getTracking(String trackingNumber) {

        Shipment shipment = shipmentRepository
                .findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRACKING_NOT_FOUND));

        TrackingResponse response = new TrackingResponse();
        response.setTrackingNumber(shipment.getTrackingNumber());
        response.setOrigin(shipment.getOrigin());
        response.setDestination(shipment.getDestination());
        response.setMode(shipment.getMode());
        response.setCurrentStatus(shipment.getStatus());

        // Customers see fixed, customer-safe text per status (from messages.properties),
        // never the internal warehouse remarks. Repeated same-status steps (for example a
        // location change) are shown once.
        List<TrackingTimelineResponse> timeline = new ArrayList<TrackingTimelineResponse>();
        ShipmentStatus previous = null;
        for (ShipmentStatusHistory history : shipment.getStatusHistory()) {
            if (history.getStatus() == previous) {
                continue;
            }
            previous = history.getStatus();

            TrackingTimelineResponse entry = new TrackingTimelineResponse();
            entry.setStatus(history.getStatus());
            entry.setPublicRemark(messages.get("tracking." + history.getStatus().name()));
            entry.setUpdatedAt(history.getUpdatedAt());
            timeline.add(entry);
        }
        response.setTimeline(timeline);

        return response;
    }

    /**
     * Sets who the shipment is for. A saved customer (customerId) wins and its name is copied
     * onto the shipment. Without an id, a typed name is still accepted. With neither, 400.
     * A customer that is switched off cannot be newly picked, but a shipment that already
     * belongs to it can still be edited.
     */
    private void applyCustomer(Shipment shipment, Long customerId, String customerName) {
        if (customerId != null) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

            boolean sameAsCurrent = shipment.getCustomer() != null
                    && customerId.equals(shipment.getCustomer().getId());
            if (!customer.isActive() && !sameAsCurrent) {
                throw new BusinessException(ErrorCode.CUSTOMER_INACTIVE);
            }

            shipment.setCustomer(customer);
            shipment.setCustomerName(customer.getName());
        } else if (hasText(customerName)) {
            shipment.setCustomer(null);
            shipment.setCustomerName(customerName.trim());
        } else {
            throw new BusinessException(ErrorCode.CUSTOMER_REQUIRED);
        }
    }

    /**
     * The one place a status changes. The rule is checked first, so a refused move
     * changes nothing and writes no history. A successful move also writes one audit row.
     */
    private ShipmentResponse changeStatus(Shipment shipment, ShipmentStatus to, String remarks, String updatedBy) {
        ShipmentStatus from = shipment.getStatus();
        ShipmentWorkflow.requireMove(shipment.getTrackingNumber(), from, to);

        shipment.setStatus(to);
        shipment.addStatusHistory(buildHistory(to, remarks, updatedBy));
        shipmentRepository.flush();   // writes now, so ids and updatedAt are filled in the reply

        audit.record(auditActionFor(to), ENTITY, shipment.getId(),
                "tracking=" + shipment.getTrackingNumber() + ", " + from + " -> " + to
                        + (hasText(remarks) ? ", remarks=" + remarks : ""));
        log.info("Status changed: id={}, from={}, to={}", shipment.getId(), from, to);
        return mapToResponse(shipment);
    }

    private AuditAction auditActionFor(ShipmentStatus to) {
        if (to == ShipmentStatus.LOADED) {
            return AuditAction.SHIPMENT_LOADED;
        }
        if (to == ShipmentStatus.DELIVERED) {
            return AuditAction.SHIPMENT_DELIVERED;
        }
        if (to == ShipmentStatus.CANCELLED) {
            return AuditAction.SHIPMENT_CANCELLED;
        }
        return AuditAction.SHIPMENT_UPDATED;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Shipment findShipmentOrThrow(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SHIPMENT_NOT_FOUND, id));
    }

    private ShipmentStatusHistory buildHistory(ShipmentStatus status, String remarks, String updatedBy) {
        ShipmentStatusHistory history = new ShipmentStatusHistory();
        history.setStatus(status);
        history.setRemarks(remarks);
        history.setUpdatedBy(CurrentUser.nameOr(updatedBy));
        history.setUpdatedAt(LocalDateTime.now());
        return history;
    }

    private ShipmentResponse mapToResponse(Shipment shipment) {
        ShipmentResponse response = new ShipmentResponse();

        response.setId(shipment.getId());
        response.setTrackingNumber(shipment.getTrackingNumber());
        response.setCustomerId(shipment.getCustomer() == null ? null : shipment.getCustomer().getId());
        response.setCustomerName(shipment.getCustomerName());
        response.setOrigin(shipment.getOrigin());
        response.setDestination(shipment.getDestination());
        response.setMode(shipment.getMode());
        response.setWeight(shipment.getWeight());
        response.setStatus(shipment.getStatus());
        response.setCreatedAt(shipment.getCreatedAt());
        response.setUpdatedAt(shipment.getUpdatedAt());

        List<StatusHistoryResponse> timeline = new ArrayList<StatusHistoryResponse>();
        for (ShipmentStatusHistory history : shipment.getStatusHistory()) {
            StatusHistoryResponse entry = new StatusHistoryResponse();
            entry.setId(history.getId());
            entry.setStatus(history.getStatus());
            entry.setRemarks(history.getRemarks());
            entry.setUpdatedBy(history.getUpdatedBy());
            entry.setUpdatedAt(history.getUpdatedAt());
            timeline.add(entry);
        }
        response.setTimeline(timeline);

        return response;
    }
}