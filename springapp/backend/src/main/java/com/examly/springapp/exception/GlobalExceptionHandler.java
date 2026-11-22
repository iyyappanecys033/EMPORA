package com.examly.springapp.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.persistence.OptimisticLockException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Centralized exception handler conforming to RFC 7807 Problem Details.
 * Standardizes API error responses across authentication, authorization,
 * domain validation, state transitions, and concurrency conflicts.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> err = new LinkedHashMap<>();
            err.put("field", error.getField());
            err.put("message", error.getDefaultMessage());
            fieldErrors.add(err);
        }

        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("errors", fieldErrors);

        String detail = fieldErrors.isEmpty()
                ? "Validation failed for request payload."
                : fieldErrors.get(0).get("field") + ": " + fieldErrors.get(0).get("message");

        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", detail,
                "https://empora.internal/errors/validation-error", request, extensions);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(),
                "https://empora.internal/errors/constraint-violation", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException e, HttpServletRequest request) {
        String msg = e.getMessage() != null ? e.getMessage() : "You do not have permission to access this resource.";
        return buildProblemDetail(HttpStatus.FORBIDDEN, "Forbidden", msg,
                "https://empora.internal/errors/forbidden", request, null);
    }

    @ExceptionHandler(UnauthorizedScopeException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedScopeException(
            UnauthorizedScopeException e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.FORBIDDEN, "Forbidden", e.getMessage(),
                "https://empora.internal/errors/scope-unauthorized", request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication required or invalid credentials.",
                "https://empora.internal/errors/unauthorized", request, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
            ResourceNotFoundException e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.NOT_FOUND, "Not Found", e.getMessage(),
                "https://empora.internal/errors/not-found", request, null);
    }

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateTransitionException(
            IllegalStateTransitionException e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", e.getMessage(),
                "https://empora.internal/errors/illegal-state-transition", request, null);
    }

    @ExceptionHandler(AntiSelfApprovalException.class)
    public ResponseEntity<Map<String, Object>> handleAntiSelfApprovalException(
            AntiSelfApprovalException e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", e.getMessage(),
                "https://empora.internal/errors/anti-self-approval", request, null);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRuleViolationException(
            BusinessRuleViolationException e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", e.getMessage(),
                "https://empora.internal/errors/business-rule-violation", request, null);
    }

    @ExceptionHandler({OptimisticLockingConflictException.class, ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<Map<String, Object>> handleOptimisticLockingConflict(
            Exception e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.CONFLICT, "Conflict",
                "The resource was modified by another concurrent transaction. Please refresh and try again.",
                "https://empora.internal/errors/concurrency-conflict", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed",
                "The requested HTTP method is not supported for this endpoint.",
                "https://empora.internal/errors/method-not-allowed", request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request",
                "Malformed JSON request body or invalid field value.",
                "https://empora.internal/errors/malformed-json", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String paramName = e.getName() != null ? e.getName() : "parameter";
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request",
                "Invalid value supplied for parameter '" + paramName + "'.",
                "https://empora.internal/errors/invalid-parameter", request, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type",
                "Unsupported Content-Type header. Use application/json.",
                "https://empora.internal/errors/unsupported-media-type", request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request",
                "Required query parameter '" + e.getParameterName() + "' is missing.",
                "https://empora.internal/errors/missing-parameter", request, null);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidSortProperty(
            PropertyReferenceException e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request",
                "Invalid sort property requested.",
                "https://empora.internal/errors/invalid-sort-property", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(
            NoResourceFoundException e, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.NOT_FOUND, "Not Found",
                "The requested resource was not found.",
                "https://empora.internal/errors/not-found", request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {
        String msg = e.getMessage() != null ? e.getMessage() : "Invalid input provided.";
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", msg,
                "https://empora.internal/errors/invalid-argument", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(
            Exception e, HttpServletRequest request) {
        log.error("Unhandled internal error on {} {}",
                request != null ? request.getMethod() : "UNKNOWN",
                request != null ? request.getRequestURI() : "UNKNOWN", e);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected internal error occurred.",
                "https://empora.internal/errors/internal-error", request, null);
    }

    private ResponseEntity<Map<String, Object>> buildProblemDetail(
            HttpStatus status, String title, String detail, String type,
            HttpServletRequest request, Map<String, Object> extensions) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type != null ? type : "about:blank");
        body.put("title", title);
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("instance", request != null ? request.getRequestURI() : "");
        body.put("timestamp", LocalDateTime.now().toString());
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            body.put("correlationId", correlationId);
        }
        body.put("error", title);
        body.put("message", detail);

        if (extensions != null && !extensions.isEmpty()) {
            body.putAll(extensions);
        }
        return new ResponseEntity<>(body, status);
    }
}
