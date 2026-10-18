package com.devbridge.logicore.exception;

import com.devbridge.logicore.dto.ApiErrorResponse;
import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.enums.ShipmentStatus;
import com.devbridge.logicore.message.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = handler();

    private static GlobalExceptionHandler handler() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        return new GlobalExceptionHandler(new MessageService(source));
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest("GET", "/api/shipments/5");
    }

    @Test
    void notFoundIs404WithMessageFromProperties() {
        ResponseEntity<ApiErrorResponse> res = handler.handleBusiness(
                new ResourceNotFoundException(ErrorCode.SHIPMENT_NOT_FOUND, 5L), request());
        assertEquals(404, res.getStatusCodeValue());
        assertEquals("SHIPMENT_NOT_FOUND", res.getBody().getCode());
        assertEquals("Shipment 5 was not found.", res.getBody().getMessage());
        assertEquals("/api/shipments/5", res.getBody().getPath());
    }

    @Test
    void invalidTransitionIs409() {
        ResponseEntity<ApiErrorResponse> res = handler.handleBusiness(
                new InvalidStatusTransitionException("LC-1",
                        ShipmentStatus.CANCELLED, ShipmentStatus.NEW), request());
        assertEquals(409, res.getStatusCodeValue());
    }

    @Test
    void accessDeniedIs403NotInternalError() {
        ResponseEntity<ApiErrorResponse> res = handler.handleDenied(new AccessDeniedException("x"), request());
        assertEquals(403, res.getStatusCodeValue());
    }

    @Test
    void unexpectedErrorIs500AndDoesNotLeakTheMessage() {
        ResponseEntity<ApiErrorResponse> res = handler.handleUnexpected(
                new RuntimeException("jdbc password=secret"), request());
        assertEquals(500, res.getStatusCodeValue());
        assertFalse(res.getBody().getMessage().contains("secret"));
    }
}