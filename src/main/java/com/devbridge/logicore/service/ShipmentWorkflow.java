package com.devbridge.logicore.service;

import com.devbridge.logicore.enums.ShipmentStatus;
import com.devbridge.logicore.exception.InvalidStatusTransitionException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.devbridge.logicore.enums.ShipmentStatus.*;

/**
 * The only place that says which status can follow which. To change a rule, change it here.
 */
public final class ShipmentWorkflow {

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED =
            new EnumMap<ShipmentStatus, Set<ShipmentStatus>>(ShipmentStatus.class);

    static {
        ALLOWED.put(NEW, EnumSet.of(RECEIVED, CANCELLED));
        ALLOWED.put(RECEIVED, EnumSet.of(IN_WAREHOUSE, CANCELLED));
        // IN_WAREHOUSE -> IN_WAREHOUSE lets staff move freight to another location.
        ALLOWED.put(IN_WAREHOUSE, EnumSet.of(IN_WAREHOUSE, LOADED, IN_TRANSIT, CANCELLED));
        ALLOWED.put(LOADED, EnumSet.of(IN_TRANSIT, CANCELLED));
        ALLOWED.put(IN_TRANSIT, EnumSet.of(DELIVERED));
        ALLOWED.put(DELIVERED, EnumSet.noneOf(ShipmentStatus.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(ShipmentStatus.class));
    }

    private ShipmentWorkflow() {
    }

    public static boolean canMove(ShipmentStatus from, ShipmentStatus to) {
        return from != null && to != null && ALLOWED.get(from).contains(to);
    }

    public static Set<ShipmentStatus> nextOf(ShipmentStatus from) {
        return Collections.unmodifiableSet(ALLOWED.get(from));
    }

    /** Throws InvalidStatusTransitionException (HTTP 409) if the move is not allowed. */
    public static void requireMove(String trackingNumber, ShipmentStatus from, ShipmentStatus to) {
        if (!canMove(from, to)) {
            throw new InvalidStatusTransitionException(trackingNumber, from, to);
        }
    }
}