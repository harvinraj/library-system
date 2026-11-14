package com.library.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.error("Resource not found exception caught: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setType(URI.create("https://example.com/resource-not-found"));
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setProperty("errorCode", "RESOURCE_NOT_FOUND");
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }

    @ExceptionHandler({BookNotAvailableException.class, InvalidException.class})
    public ProblemDetail handleBusinessRuleViolation(RuntimeException ex) {
        log.error("Business rule violation caught: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setType(URI.create("https://example.com/business-rule-violation"));
        problemDetail.setTitle("Business Rule Violation");
        problemDetail.setProperty("errorCode", "BUSINESS_RULE_VIOLATION");
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Request validation failed: {} field error(s)", ex.getBindingResult().getErrorCount());

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for request payload");
        problemDetail.setType(URI.create("https://example.com/invalid-request-parameters"));
        problemDetail.setTitle("Invalid Request Parameters");
        problemDetail.setProperty("errorCode", "INVALID_REQUEST_PARAMETERS");
        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }

    @ExceptionHandler (Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred.");
        problemDetail.setType(URI.create("https://example.com/internal-server-error"));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("errorCode", "INTERNAL_SERVER_ERROR");
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }
}