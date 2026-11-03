package com.devbridge.logicore.service.impl;

import com.devbridge.logicore.dto.CancelShipmentRequest;
import com.devbridge.logicore.dto.CreateShipmentRequest;
import com.devbridge.logicore.dto.PagedResponse;
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
import com.devbridge.logicore.model.Warehouse;
import com.devbridge.logicore.repository.CustomerRepository;
import com.devbridge.logicore.repository.ShipmentRepository;
import com.devbridge.logicore.repository.ShipmentSearchRepository;
import com.devbridge.logicore.repository.ShipmentSpecs;
import com.devbridge.logicore.repository.WarehouseRepository;
import com.devbridge.logicore.security.CurrentUser;
import com.devbridge.logicore.security.Scope;
import com.devbridge.logicore.service.AuditService;
import com.devbridge.logicore.service.CustomsService;
import com.devbridge.logicore.service.ReceiptHoldService;
import com.devbridge.logicore.service.ScopeService;
import com.devbridge.logicore.service.ShipmentService;
import com.devbridge.logicore.service.ShipmentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class ShipmentServiceImpl implements ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentServiceImpl.class);
    private static final String ENTITY = "SHIPMENT";

    // Paging limits, and the only columns a client may sort by.
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE = new HashSet<String>(
            Arrays.asList("trackingNumber", "customerName", "createdAt", "status"));

    private final ShipmentRepository shipmentRepository;
    private final ShipmentSearchRepository searchRepository;
    private final CustomerRepository customerRepository;
    private final WarehouseRepository warehouseRepository;
    private final ScopeService scopeService;
    private final MessageService messages;
    private final AuditService audit;
    private final ReceiptHoldService holds;
    private final CustomsService customs;

    public ShipmentServiceImpl(ShipmentRepository shipmentRepository,
                               ShipmentSearchRepository searchRepository,
                               CustomerRepository customerRepository,
                               WarehouseRepository warehouseRepository,
                               ScopeService scopeService,
                               MessageService messages,
                               AuditService audit,
                               ReceiptHoldService holds,
                               CustomsService customs) {
        this.shipmentRepository = shipmentRepository;
        this.searchRepository = searchRepository;
        this.customerRepository = customerRepository;
        this.warehouseRepository = warehouseRepository;
        this.scopeService = scopeService;
        this.messages = messages;
        this.audit = audit;
        this.holds = holds;
        this.customs = customs;
    }

    @Override
    public ShipmentResponse createShipment(CreateShipmentRequest request) {

        Shipment shipment = new Shipment();

        applyCustomer(shipment, request.getCustomerId(), request.getCustomerName());
        applyWarehouses(shipment, request.getOriginWarehouseId(), request.getDestinationWarehouseId());

        shipment.setTrackingNumber("TRK-" + System.currentTimeMillis());
        shipment.setOrigin(request.getOrigin());
        shipment.setDestination(request.getDestination());
        shipment.setMode(request.getMode());
        shipment.setReferenceNumber(cleanReference(request.getReferenceNumber()));
        shipment.setWeight(request.getWeight());
        shipment.setStatus(ShipmentStatus.NEW);

        // The creator must be able to see what they create, so a shipment outside
        // their own region or warehouse is refused.
        requireVisible(shipment);

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

        List<Shipment> shipments = loadVisibleShipments();
        List<Shipment> matching = new ArrayList<Shipment>();

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
            matching.add(shipment);
        }

        return toResponses(matching, true);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ShipmentResponse> searchShipments(
            ShipmentStatus status,
            ShipmentMode mode,
            String text,
            int page,
            int size,
            String sortBy,
            String dir) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String property = SORTABLE.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        // The id is a tie-breaker, so rows with equal values never jump between pages.
        Sort sort = Sort.by(direction, property).and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        Specification<Shipment> spec = Specification
                .where(ShipmentSpecs.visibleTo(scopeService.current()))
                .and(ShipmentSpecs.hasStatus(status))
                .and(ShipmentSpecs.hasMode(mode))
                .and(ShipmentSpecs.matches(text))
                .and(ShipmentSpecs.withWarehouses());

        Page<Shipment> result = searchRepository.findAll(spec, pageable);

        // Page rows skip the full status timeline; GET /api/shipments/{id} still has it.
        return new PagedResponse<ShipmentResponse>(
                toResponses(result.getContent(), false),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
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
        applyWarehouses(shipment, request.getOriginWarehouseId(), request.getDestinationWarehouseId());

        shipment.setOrigin(request.getOrigin());
        shipment.setDestination(request.getDestination());
        shipment.setMode(request.getMode());
        shipment.setReferenceNumber(cleanReference(request.getReferenceNumber()));
        shipment.setWeight(request.getWeight());

        // After the change, the editor must still be allowed to see this shipment.
        requireVisible(shipment);

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

    // Public tracking is not scoped: anyone with a tracking number may look it up.
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

    /** The shipments the current user may see: everything, their region, their warehouse, or nothing. */
    private List<Shipment> loadVisibleShipments() {
        Scope scope = scopeService.current();
        switch (scope.getLevel()) {
            case ALL:
                return shipmentRepository.findAll();
            case REGION:
                return shipmentRepository.findVisibleToRegion(scope.getRegionId());
            case WAREHOUSE:
                return shipmentRepository.findVisibleToWarehouse(scope.getWarehouseId());
            default:
                return new ArrayList<Shipment>();
        }
    }

    /** Refuses a shipment the current user could not see. */
    private void requireVisible(Shipment shipment) {
        if (!scopeService.current().canSee(shipment)) {
            throw new BusinessException(ErrorCode.OUT_OF_SCOPE);
        }
    }

    /** Sets the warehouses when ids are sent. An id that is not sent leaves the value as it was. */
    private void applyWarehouses(Shipment shipment, Long originId, Long destinationId) {
        if (originId != null) {
            shipment.setOriginWarehouse(findWarehouseOrThrow(originId));
        }
        if (destinationId != null) {
            shipment.setDestinationWarehouse(findWarehouseOrThrow(destinationId));
        }
    }

    private Warehouse findWarehouseOrThrow(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.WAREHOUSE_NOT_FOUND, id));
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

    /** A blank reference number is stored as empty (null). */
    private String cleanReference(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * The one place a status changes. The rule is checked first, so a refused move
     * changes nothing and writes no history. A successful move also writes one audit row.
     * Before a shipment is loaded or sent, any receiving problem must be cleared by a manager
     * and an international shipment must be cleared through customs.
     */
    private ShipmentResponse changeStatus(Shipment shipment, ShipmentStatus to, String remarks, String updatedBy) {
        ShipmentStatus from = shipment.getStatus();
        ShipmentWorkflow.requireMove(shipment.getTrackingNumber(), from, to);
        if (to == ShipmentStatus.LOADED || to == ShipmentStatus.IN_TRANSIT) {
            holds.requireNotOnHold(shipment);
            customs.requireCleared(shipment);
        }

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

    /** Finds a shipment the current user may see. Outside their scope it is reported as not found. */
    private Shipment findShipmentOrThrow(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SHIPMENT_NOT_FOUND, id));
        if (!scopeService.current().canSee(shipment)) {
            throw new ResourceNotFoundException(ErrorCode.SHIPMENT_NOT_FOUND, id);
        }
        return shipment;
    }

    private ShipmentStatusHistory buildHistory(ShipmentStatus status, String remarks, String updatedBy) {
        ShipmentStatusHistory history = new ShipmentStatusHistory();
        history.setStatus(status);
        history.setRemarks(remarks);
        history.setUpdatedBy(CurrentUser.nameOr(updatedBy));
        history.setUpdatedAt(LocalDateTime.now());
        return history;
    }

    /** One shipment, with its full timeline. Used by the single-shipment endpoints. */
    private ShipmentResponse mapToResponse(Shipment shipment) {
        // A hold only matters before the freight is loaded, so later statuses skip the lookup.
        boolean beforeLoading = shipment.getStatus() == ShipmentStatus.RECEIVED
                || shipment.getStatus() == ShipmentStatus.IN_WAREHOUSE;
        boolean onHold = beforeLoading && holds.isOnHold(shipment);
        return toResponse(shipment, onHold, customs.statusOf(shipment), true);
    }

    /** Many shipments: the hold flags and customs statuses are each read with one query. */
    private List<ShipmentResponse> toResponses(List<Shipment> shipments, boolean includeTimeline) {
        Set<Long> onHold = holds.onHoldAmong(shipments);
        Map<Long, String> customsStatuses = customs.statusesOf(shipments);

        List<ShipmentResponse> responses = new ArrayList<ShipmentResponse>();
        for (Shipment shipment : shipments) {
            responses.add(toResponse(
                    shipment,
                    onHold.contains(shipment.getId()),
                    customsStatuses.get(shipment.getId()),
                    includeTimeline));
        }
        return responses;
    }

    private ShipmentResponse toResponse(Shipment shipment, boolean onHold, String customsStatus,
                                        boolean includeTimeline) {
        ShipmentResponse response = new ShipmentResponse();

        response.setId(shipment.getId());
        response.setTrackingNumber(shipment.getTrackingNumber());
        response.setCustomerId(shipment.getCustomer() == null ? null : shipment.getCustomer().getId());
        response.setCustomerName(shipment.getCustomerName());
        response.setOrigin(shipment.getOrigin());
        response.setDestination(shipment.getDestination());
        if (shipment.getOriginWarehouse() != null) {
            response.setOriginWarehouseId(shipment.getOriginWarehouse().getId());
            response.setOriginWarehouseName(shipment.getOriginWarehouse().getName());
        }
        if (shipment.getDestinationWarehouse() != null) {
            response.setDestinationWarehouseId(shipment.getDestinationWarehouse().getId());
            response.setDestinationWarehouseName(shipment.getDestinationWarehouse().getName());
        }
        response.setMode(shipment.getMode());
        response.setReferenceNumber(shipment.getReferenceNumber());
        response.setWeight(shipment.getWeight());
        response.setStatus(shipment.getStatus());
        response.setOnHold(onHold);
        response.setCustomsStatus(customsStatus);
        response.setCreatedAt(shipment.getCreatedAt());
        response.setUpdatedAt(shipment.getUpdatedAt());

        if (includeTimeline) {
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
        }

        return response;
    }
}