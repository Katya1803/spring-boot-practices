package com.katya.app.common.exception;

import com.katya.app.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler for all REST controllers
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle BusinessException (expected business logic errors)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("Business exception: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error(ex.getErrorCode())
                .message(ex.getMessage())
                .traceId(MDC.get("traceId"))
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle ValidationException (custom validation errors)
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {

        log.warn("Validation exception: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        List<ErrorResponse.ValidationError> errors = new ArrayList<>();
        ex.getFieldErrors().forEach((field, message) -> errors.add(ErrorResponse.ValidationError.builder()
                .field(field)
                .message(message)
                .build()));

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error("VALIDATION_ERROR")
                .message("Validation failed")
                .validationErrors(errors)
                .traceId(MDC.get("traceId"))
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle Bean Validation errors (from @Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("Validation error on path: {}", request.getRequestURI());

        List<ErrorResponse.ValidationError> errors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            Object rejectedValue = ((FieldError) error).getRejectedValue();

            errors.add(ErrorResponse.ValidationError.builder()
                    .field(fieldName)
                    .message(errorMessage)
                    .rejectedValue(rejectedValue)
                    .build());
        });

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error("VALIDATION_ERROR")
                .message("Invalid input data")
                .validationErrors(errors)
                .traceId(MDC.get("traceId"))
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle SystemException (technical errors)
     */
    @ExceptionHandler(SystemException.class)
    public ResponseEntity<ErrorResponse> handleSystemException(
            SystemException ex, HttpServletRequest request) {

        log.error("System exception: {} - Path: {}", ex.getMessage(), request.getRequestURI(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error(ex.getErrorCode())
                .message("An internal error occurred. Please try again later.")
                .traceId(MDC.get("traceId"))
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle all other unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected exception: {} - Path: {}", ex.getMessage(), request.getRequestURI(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .traceId(MDC.get("traceId"))
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}