package com.devbridge.logicore.exception;

import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.enums.ShipmentStatus;

public class InvalidStatusTransitionException extends BusinessException {

    public InvalidStatusTransitionException(String trackingNumber, ShipmentStatus from, ShipmentStatus to) {
        super(ErrorCode.INVALID_STATUS_TRANSITION, trackingNumber, from, to);
    }
}