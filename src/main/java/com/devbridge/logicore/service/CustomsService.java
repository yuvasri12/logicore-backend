package com.devbridge.logicore.service;

import com.devbridge.logicore.enums.AuditAction;
import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.exception.BusinessException;
import com.devbridge.logicore.exception.ResourceNotFoundException;
import com.devbridge.logicore.model.CustomsClearance;
import com.devbridge.logicore.model.Shipment;
import com.devbridge.logicore.model.Warehouse;
import com.devbridge.logicore.repository.CustomsClearanceRepository;
import com.devbridge.logicore.repository.ShipmentRepository;
import com.devbridge.logicore.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One rule: a shipment that crosses a country border needs customs clearance before it can be
 * loaded or dispatched. "Crosses a border" means the origin and destination warehouses are in
 * different countries. Loading and dispatching call requireCleared, so the rule lives in one place.
 */
@Service
@Transactional
public class CustomsService {

    public static final String NOT_REQUIRED = "NOT_REQUIRED";
    public static final String PENDING = "PENDING";
    public static final String CLEARED = "CLEARED";

    private static final String ENTITY_SHIPMENT = "SHIPMENT";

    private final CustomsClearanceRepository clearanceRepository;
    private final ShipmentRepository shipmentRepository;
    private final ScopeService scopeService;
    private final AuditService auditService;

    public CustomsService(CustomsClearanceRepository clearanceRepository,
                          ShipmentRepository shipmentRepository,
                          ScopeService scopeService,
                          AuditService auditService) {
        this.clearanceRepository = clearanceRepository;
        this.shipmentRepository = shipmentRepository;
        this.scopeService = scopeService;
        this.auditService = auditService;
    }

    /** True when both warehouses are known and are in different countries. */
    @Transactional(readOnly = true)
    public boolean isRequired(Shipment shipment) {
        Warehouse origin = shipment.getOriginWarehouse();
        Warehouse destination = shipment.getDestinationWarehouse();
        if (origin == null || destination == null) {
            return false;
        }
        String from = clean(origin.getCountry());
        String to = clean(destination.getCountry());
        return !from.isEmpty() && !to.isEmpty() && !from.equalsIgnoreCase(to);
    }

    /** NOT_REQUIRED, PENDING or CLEARED. The database is only asked for international shipments. */
    @Transactional(readOnly = true)
    public String statusOf(Shipment shipment) {
        if (!isRequired(shipment)) {
            return NOT_REQUIRED;
        }
        return clearanceRepository.existsByShipment(shipment) ? CLEARED : PENDING;
    }

    /** The same answer for many shipments at once, using a single query. Key = shipment id. */
    @Transactional(readOnly = true)
    public Map<Long, String> statusesOf(Collection<Shipment> shipments) {
        Map<Long, String> result = new HashMap<Long, String>();
        List<Long> international = new ArrayList<Long>();
        for (Shipment shipment : shipments) {
            if (isRequired(shipment)) {
                international.add(shipment.getId());
            } else {
                result.put(shipment.getId(), NOT_REQUIRED);
            }
        }

        Set<Long> cleared = new HashSet<Long>();
        if (!international.isEmpty()) {
            cleared.addAll(clearanceRepository.findClearedShipmentIds(international));
        }
        for (Long id : international) {
            result.put(id, cleared.contains(id) ? CLEARED : PENDING);
        }
        return result;
    }

    /** Throws CUSTOMS_NOT_CLEARED (HTTP 409) while an international shipment is not cleared. */
    @Transactional(readOnly = true)
    public void requireCleared(Shipment shipment) {
        if (PENDING.equals(statusOf(shipment))) {
            throw new BusinessException(ErrorCode.CUSTOMS_NOT_CLEARED);
        }
    }

    /** A manager records that customs has cleared the shipment. Recorded in the audit log. */
    public void clear(Long shipmentId, String customsReference, String note) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SHIPMENT_NOT_FOUND, shipmentId));
        if (!scopeService.current().canSee(shipment)) {
            throw new ResourceNotFoundException(ErrorCode.SHIPMENT_NOT_FOUND, shipmentId);
        }
        if (!isRequired(shipment)) {
            throw new BusinessException(ErrorCode.CUSTOMS_NOT_REQUIRED);
        }
        if (clearanceRepository.existsByShipment(shipment)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ACTION);
        }

        CustomsClearance clearance = new CustomsClearance();
        clearance.setShipment(shipment);
        clearance.setCustomsReference(customsReference.trim());
        clearance.setClearedBy(CurrentUser.nameOr("SYSTEM"));
        clearance.setClearedAt(LocalDateTime.now());
        clearance.setNote(note == null || note.trim().isEmpty() ? null : note.trim());
        clearanceRepository.save(clearance);

        auditService.record(AuditAction.CUSTOMS_CLEARED, ENTITY_SHIPMENT, shipment.getId(),
                "tracking=" + shipment.getTrackingNumber()
                        + ", customsReference=" + clearance.getCustomsReference());
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}