package com.devbridge.logicore.service.impl;

import com.devbridge.logicore.dto.AssignLocationRequest;
import com.devbridge.logicore.dto.DispatchRequest;
import com.devbridge.logicore.dto.DispatchResponse;
import com.devbridge.logicore.dto.FreightAssignmentResponse;
import com.devbridge.logicore.dto.WarehouseReceiptResponse;
import com.devbridge.logicore.dto.WarehouseReceiveRequest;
import com.devbridge.logicore.enums.AuditAction;
import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.enums.ShipmentStatus;
import com.devbridge.logicore.exception.ResourceNotFoundException;
import com.devbridge.logicore.model.Dispatch;
import com.devbridge.logicore.model.FreightAssignment;
import com.devbridge.logicore.model.Shipment;
import com.devbridge.logicore.model.ShipmentStatusHistory;
import com.devbridge.logicore.model.StorageLocation;
import com.devbridge.logicore.model.Warehouse;
import com.devbridge.logicore.model.WarehouseReceipt;
import com.devbridge.logicore.repository.DispatchRepository;
import com.devbridge.logicore.repository.FreightAssignmentRepository;
import com.devbridge.logicore.repository.ShipmentRepository;
import com.devbridge.logicore.repository.StorageLocationRepository;
import com.devbridge.logicore.repository.WarehouseReceiptRepository;
import com.devbridge.logicore.security.CurrentUser;
import com.devbridge.logicore.service.AuditService;
import com.devbridge.logicore.service.CustomsService;
import com.devbridge.logicore.service.ReceiptHoldService;
import com.devbridge.logicore.service.ScopeService;
import com.devbridge.logicore.service.ShipmentWorkflow;
import com.devbridge.logicore.service.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseServiceImpl.class);
    private static final String ENTITY_SHIPMENT = "SHIPMENT";

    private final ShipmentRepository shipmentRepository;
    private final WarehouseReceiptRepository warehouseReceiptRepository;
    private final FreightAssignmentRepository freightAssignmentRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final DispatchRepository dispatchRepository;
    private final AuditService auditService;
    private final ScopeService scopeService;
    private final ReceiptHoldService holds;
    private final CustomsService customs;

    public WarehouseServiceImpl(
            ShipmentRepository shipmentRepository,
            WarehouseReceiptRepository warehouseReceiptRepository,
            FreightAssignmentRepository freightAssignmentRepository,
            StorageLocationRepository storageLocationRepository,
            DispatchRepository dispatchRepository,
            AuditService auditService,
            ScopeService scopeService,
            ReceiptHoldService holds,
            CustomsService customs) {
        this.shipmentRepository = shipmentRepository;
        this.warehouseReceiptRepository = warehouseReceiptRepository;
        this.freightAssignmentRepository = freightAssignmentRepository;
        this.storageLocationRepository = storageLocationRepository;
        this.dispatchRepository = dispatchRepository;
        this.auditService = auditService;
        this.scopeService = scopeService;
        this.holds = holds;
        this.customs = customs;
    }

    @Override
    public WarehouseReceiptResponse receive(WarehouseReceiveRequest request) {

        Shipment shipment = findVisibleShipment(request.getShipmentId());

        ShipmentWorkflow.requireMove(
                shipment.getTrackingNumber(), shipment.getStatus(), ShipmentStatus.RECEIVED);

        WarehouseReceipt receipt = new WarehouseReceipt();
        receipt.setShipment(shipment);
        receipt.setCondition(request.getCondition());
        receipt.setWeightChecked(request.getWeightChecked());
        receipt.setReceivedBy(CurrentUser.nameOr(request.getReceivedBy()));
        receipt.setReceivedAt(LocalDateTime.now());

        WarehouseReceipt savedReceipt = warehouseReceiptRepository.save(receipt);

        shipment.setStatus(ShipmentStatus.RECEIVED);
        shipment.addStatusHistory(buildHistory(
                ShipmentStatus.RECEIVED,
                "Freight received in condition " + request.getCondition(),
                request.getReceivedBy()));
        shipmentRepository.save(shipment);

        // Audit entry, saved in the same transaction as the receipt.
        auditService.record(AuditAction.FREIGHT_RECEIVED, ENTITY_SHIPMENT, shipment.getId(),
                "tracking=" + shipment.getTrackingNumber()
                        + ", condition=" + request.getCondition()
                        + ", weightChecked=" + request.getWeightChecked());

        WarehouseReceiptResponse response = new WarehouseReceiptResponse();
        response.setId(savedReceipt.getId());
        response.setShipmentId(shipment.getId());
        response.setCondition(savedReceipt.getCondition());
        response.setWeightChecked(savedReceipt.getWeightChecked());
        response.setReceivedBy(savedReceipt.getReceivedBy());
        response.setReceivedAt(savedReceipt.getReceivedAt());

        log.info("Receipt saved: shipmentId={}, condition={}", shipment.getId(), request.getCondition());

        return response;
    }

    @Override
    public FreightAssignmentResponse assignLocation(AssignLocationRequest request) {

        Shipment shipment = findVisibleShipment(request.getShipmentId());

        ShipmentWorkflow.requireMove(
                shipment.getTrackingNumber(), shipment.getStatus(), ShipmentStatus.IN_WAREHOUSE);

        // The freight is stored at the warehouse where the shipment starts.
        Warehouse warehouse = shipment.getOriginWarehouse();
        StorageLocation location = findOrCreateLocation(
                warehouse, request.getZone(), request.getRack(), request.getBin());

        FreightAssignment assignment = new FreightAssignment();
        assignment.setShipment(shipment);
        assignment.setStorageLocation(location);
        assignment.setAssignedAt(LocalDateTime.now());

        FreightAssignment savedAssignment = freightAssignmentRepository.save(assignment);

        shipment.setStatus(ShipmentStatus.IN_WAREHOUSE);
        shipment.addStatusHistory(buildHistory(
                ShipmentStatus.IN_WAREHOUSE,
                "Assigned to zone " + request.getZone() + ", rack " + request.getRack() + ", bin " + request.getBin(),
                request.getAssignedBy()));
        shipmentRepository.save(shipment);

        auditService.record(AuditAction.LOCATION_ASSIGNED, ENTITY_SHIPMENT, shipment.getId(),
                "tracking=" + shipment.getTrackingNumber()
                        + (warehouse == null ? "" : ", warehouse=" + warehouse.getCode())
                        + ", location=" + request.getZone() + "/" + request.getRack() + "/" + request.getBin());

        FreightAssignmentResponse response = new FreightAssignmentResponse();
        response.setId(savedAssignment.getId());
        response.setShipmentId(shipment.getId());
        response.setZone(location.getZone());
        response.setRack(location.getRack());
        response.setBin(location.getBin());
        response.setAssignedAt(savedAssignment.getAssignedAt());

        log.info("Location assigned: shipmentId={}", shipment.getId());

        return response;
    }

    @Override
    public DispatchResponse dispatch(DispatchRequest request) {

        Shipment shipment = findVisibleShipment(request.getShipmentId());

        ShipmentWorkflow.requireMove(
                shipment.getTrackingNumber(), shipment.getStatus(), ShipmentStatus.IN_TRANSIT);

        // Freight with an open receiving problem, or an international shipment that has not
        // cleared customs, cannot leave.
        holds.requireNotOnHold(shipment);
        customs.requireCleared(shipment);

        Dispatch dispatch = new Dispatch();
        dispatch.setShipment(shipment);
        dispatch.setVehicleNumber(request.getVehicleNumber());
        dispatch.setDestination(request.getDestination());
        dispatch.setDispatchedAt(LocalDateTime.now());

        Dispatch savedDispatch = dispatchRepository.save(dispatch);

        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        shipment.addStatusHistory(buildHistory(
                ShipmentStatus.IN_TRANSIT,
                "Dispatched on vehicle " + request.getVehicleNumber() + " to " + request.getDestination(),
                request.getDispatchedBy()));
        shipmentRepository.save(shipment);

        auditService.record(AuditAction.SHIPMENT_DISPATCHED, ENTITY_SHIPMENT, shipment.getId(),
                "tracking=" + shipment.getTrackingNumber()
                        + ", vehicle=" + request.getVehicleNumber()
                        + ", destination=" + request.getDestination());

        DispatchResponse response = new DispatchResponse();
        response.setId(savedDispatch.getId());
        response.setShipmentId(shipment.getId());
        response.setVehicleNumber(savedDispatch.getVehicleNumber());
        response.setDestination(savedDispatch.getDestination());
        response.setDispatchedAt(savedDispatch.getDispatchedAt());

        log.info("Dispatched: shipmentId={}", shipment.getId());

        return response;
    }

    /**
     * Finds the bin inside this warehouse, or creates it. The same zone/rack/bin name can exist in
     * different warehouses. A shipment with no warehouse (old data) uses the old warehouse-less rows.
     */
    private StorageLocation findOrCreateLocation(Warehouse warehouse, String zone, String rack, String bin) {
        Optional<StorageLocation> existing = warehouse == null
                ? storageLocationRepository.findByWarehouseIsNullAndZoneAndRackAndBin(zone, rack, bin)
                : storageLocationRepository.findByWarehouseAndZoneAndRackAndBin(warehouse, zone, rack, bin);
        if (existing.isPresent()) {
            return existing.get();
        }
        StorageLocation created = new StorageLocation();
        created.setWarehouse(warehouse);
        created.setZone(zone);
        created.setRack(rack);
        created.setBin(bin);
        return storageLocationRepository.save(created);
    }

    /** Finds a shipment the current user may see. Outside their scope it is reported as not found. */
    private Shipment findVisibleShipment(Long id) {
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
}