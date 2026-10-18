package com.devbridge.logicore.exception;

import com.devbridge.logicore.config.RequestIdFilter;
import com.devbridge.logicore.dto.ApiErrorResponse;
import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.message.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageService messages;

    public GlobalExceptionHandler(MessageService messages) {
        this.messages = messages;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        ErrorCode code = ex.getErrorCode();
        log.warn("Request refused: code={}, path={}", code, req.getRequestURI());
        return build(code, req, Collections.<String>emptyList(), ex.getArgs());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex,
                                                              HttpServletRequest req) {
        List<String> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                details.add(fe.getField() + ": " + fe.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(ge ->
                details.add(ge.getObjectName() + ": " + ge.getDefaultMessage()));
        log.warn("Validation failed: path={}, errors={}", req.getRequestURI(), details.size());
        return build(ErrorCode.VALIDATION_FAILED, req, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex,
                                                             HttpServletRequest req) {
        List<String> details = new ArrayList<>();
        ex.getConstraintViolations().forEach(v ->
                details.add(v.getPropertyPath() + ": " + v.getMessage()));
        log.warn("Validation failed: path={}, errors={}", req.getRequestURI(), details.size());
        return build(ErrorCode.VALIDATION_FAILED, req, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                             HttpServletRequest req) {
        log.warn("Unreadable request body: path={}", req.getRequestURI());
        return build(ErrorCode.REQUEST_UNREADABLE, req, Collections.<String>emptyList());
    }

    // Must exist, or the catch-all below turns a 403 into a 500.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Access denied: path={}", req.getRequestURI());
        return build(ErrorCode.ACCESS_DENIED, req, Collections.<String>emptyList());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error: path={}", req.getRequestURI(), ex);
        return build(ErrorCode.UNEXPECTED, req, Collections.<String>emptyList());
    }

    private ResponseEntity<ApiErrorResponse> build(ErrorCode code, HttpServletRequest req,
                                                   List<String> details, Object... args) {
        ApiErrorResponse body = new ApiErrorResponse(
                code.getStatus().value(),
                code.getStatus().getReasonPhrase(),
                code.name(),
                messages.get(code.getMessageKey(), args),
                details,
                req.getRequestURI(),
                MDC.get(RequestIdFilter.MDC_KEY));
        return ResponseEntity.status(code.getStatus()).body(body);
    }
}