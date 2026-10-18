package com.devbridge.logicore.service;

import com.devbridge.logicore.enums.ShipmentStatus;
import com.devbridge.logicore.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import static com.devbridge.logicore.enums.ShipmentStatus.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipmentWorkflowTest {

    @Test
    void normalPathIsAllowed() {
        assertTrue(ShipmentWorkflow.canMove(NEW, RECEIVED));
        assertTrue(ShipmentWorkflow.canMove(RECEIVED, IN_WAREHOUSE));
        assertTrue(ShipmentWorkflow.canMove(IN_WAREHOUSE, LOADED));
        assertTrue(ShipmentWorkflow.canMove(LOADED, IN_TRANSIT));
        assertTrue(ShipmentWorkflow.canMove(IN_TRANSIT, DELIVERED));
    }

    @Test
    void stepsCannotBeSkipped() {
        assertFalse(ShipmentWorkflow.canMove(NEW, IN_TRANSIT));
        assertFalse(ShipmentWorkflow.canMove(NEW, DELIVERED));
        assertFalse(ShipmentWorkflow.canMove(RECEIVED, DELIVERED));
    }

    @Test
    void cannotGoBackwards() {
        assertFalse(ShipmentWorkflow.canMove(DELIVERED, NEW));
        assertFalse(ShipmentWorkflow.canMove(IN_TRANSIT, IN_WAREHOUSE));
    }

    @Test
    void deliveredAndCancelledAreFinal() {
        for (ShipmentStatus target : ShipmentStatus.values()) {
            assertFalse(ShipmentWorkflow.canMove(DELIVERED, target));
            assertFalse(ShipmentWorkflow.canMove(CANCELLED, target));
        }
    }

    @Test
    void cannotCancelOnceInTransit() {
        assertFalse(ShipmentWorkflow.canMove(IN_TRANSIT, CANCELLED));
        assertTrue(ShipmentWorkflow.canMove(LOADED, CANCELLED));
    }

    @Test
    void receivingTwiceIsRefused() {
        assertFalse(ShipmentWorkflow.canMove(RECEIVED, RECEIVED));
    }

    @Test
    void freightCanBeMovedToAnotherLocation() {
        assertTrue(ShipmentWorkflow.canMove(IN_WAREHOUSE, IN_WAREHOUSE));
    }

    @Test
    void requireMoveThrowsForBadMoveAndPassesForGoodMove() {
        assertThrows(InvalidStatusTransitionException.class,
                () -> ShipmentWorkflow.requireMove("TRK-1", DELIVERED, NEW));
        assertDoesNotThrow(() -> ShipmentWorkflow.requireMove("TRK-1", NEW, RECEIVED));
    }
}