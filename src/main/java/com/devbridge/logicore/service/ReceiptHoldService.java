package com.devbridge.logicore.service;

import com.devbridge.logicore.enums.AuditAction;
import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.exception.BusinessException;
import com.devbridge.logicore.exception.ResourceNotFoundException;
import com.devbridge.logicore.model.Shipment;
import com.devbridge.logicore.model.WarehouseReceipt;
import com.devbridge.logicore.repository.ShipmentRepository;
import com.devbridge.logicore.repository.WarehouseReceiptRepository;
import com.devbridge.logicore.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * One rule: freight received with a problem is on hold until an Ops Manager or Admin clears it.
 * Loading and dispatching call requireNotOnHold, so the rule lives in one place.
 */
@Service
@Transactional
public class ReceiptHoldService {

    private static final String ENTITY_SHIPMENT = "SHIPMENT";

    private final WarehouseReceiptRepository receiptRepository;
    private final ShipmentRepository shipmentRepository;
    private final ScopeService scopeService;
    private final AuditService auditService;

    public ReceiptHoldService(WarehouseReceiptRepository receiptRepository,
                              ShipmentRepository shipmentRepository,
                              ScopeService scopeService,
                              AuditService auditService) {
        this.receiptRepository = receiptRepository;
        this.shipmentRepository = shipmentRepository;
        this.scopeService = scopeService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public boolean isOnHold(Shipment shipment) {
        Optional<WarehouseReceipt> latest = receiptRepository.findFirstByShipmentOrderByReceivedAtDescIdDesc(shipment);
        return latest.isPresent() && latest.get().hasOpenIssue();
    }

    /** Throws FREIGHT_ON_HOLD (HTTP 409) while the latest receipt has an open issue. */
    @Transactional(readOnly = true)
    public void requireNotOnHold(Shipment shipment) {
        if (isOnHold(shipment)) {
            throw new BusinessException(ErrorCode.FREIGHT_ON_HOLD);
        }
    }

    /** A manager accepts the problem, with a reason. Recorded in the audit log. */
    public void clearIssue(Long shipmentId, String note) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SHIPMENT_NOT_FOUND, shipmentId));
        if (!scopeService.current().canSee(shipment)) {
            throw new ResourceNotFoundException(ErrorCode.SHIPMENT_NOT_FOUND, shipmentId);
        }

        WarehouseReceipt receipt = receiptRepository.findFirstByShipmentOrderByReceivedAtDescIdDesc(shipment)
                .filter(WarehouseReceipt::hasOpenIssue)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_OPEN_ISSUE));

        receipt.setIssueClearedBy(CurrentUser.nameOr("SYSTEM"));
        receipt.setIssueClearedAt(LocalDateTime.now());
        receipt.setIssueNote(note.trim());
        receiptRepository.save(receipt);

        auditService.record(AuditAction.RECEIPT_ISSUE_CLEARED, ENTITY_SHIPMENT, shipment.getId(),
                "tracking=" + shipment.getTrackingNumber()
                        + ", condition=" + receipt.getCondition()
                        + ", note=" + receipt.getIssueNote());
    }
}