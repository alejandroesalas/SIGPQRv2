package com.sigpqr.common.exception;

import com.sigpqr.common.constants.AppConstants;
import com.sigpqr.common.dto.ErrorResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleResourceAlreadyExists(ResourceAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> details.put(error.getField(), error.getDefaultMessage()));

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                details,
                MDC.get(AppConstants.CORRELATION_ID_MDC_KEY),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        ErrorResponse response = new ErrorResponse(
                status.value(),
                message,
                null,
                MDC.get(AppConstants.CORRELATION_ID_MDC_KEY),
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(response);
    }
}
