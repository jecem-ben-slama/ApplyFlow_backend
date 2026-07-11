package com.applyflow.tracker_api.config;

import com.applyflow.tracker_api.dtos.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataAccessException;

import java.net.ConnectException;
import java.util.NoSuchElementException;

@RestControllerAdvice
@Slf4j // Injects logger for clean production log streams on Render
public class GlobalExceptionHandler {

    // 1. Catch business validation rules
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 2. Catch resource lookup missing records
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException ex) {
        return new ResponseEntity<>(ApiResponse.error("Requested item not found: " + ex.getMessage()),
                HttpStatus.NOT_FOUND);
    }

    // 3. Catch database SQL state constraint failures (e.g., foreign keys)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseConstraints(DataIntegrityViolationException ex) {
        return new ResponseEntity<>(
                ApiResponse.error("Database constraint violation. Ensure foreign keys are correct."),
                HttpStatus.CONFLICT);
    }

    // NEW 4. Catch Network/Database Connection Drops explicitly (e.g., database
    // pool exhausts or goes to sleep)
    @ExceptionHandler({ DataAccessResourceFailureException.class, ConnectException.class })
    public ResponseEntity<ApiResponse<Void>> handleDatabaseConnectionErrors(Exception ex) {
        log.error("CRITICAL: Database connection failed or dropped! Context: ", ex);
        return new ResponseEntity<>(
                ApiResponse
                        .error("The database service is temporarily unavailable. Please try again in a few moments."),
                HttpStatus.SERVICE_UNAVAILABLE); // 503 Service Unavailable
    }

    // NEW 5. Catch Hibernate/Schema mapping errors (e.g., a table or column name
    // doesn't exist)
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericDatabaseErrors(DataAccessException ex) {
        log.error("DATABASE SCHEMA ERROR: Query execution or Hibernate structure matching failed: ", ex);
        return new ResponseEntity<>(
                ApiResponse
                        .error("A data access error occurred while processing your request. Please contact support."),
                HttpStatus.INTERNAL_SERVER_ERROR); // 500 Internal Server Error
    }

    // 6. Catch your SecurityContextService 401 exceptions cleanly
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getReason() != null ? ex.getReason() : ex.getMessage())
                .data(null)
                .build();

        return new ResponseEntity<>(response, ex.getStatusCode());
    }

    // 7. The SINGLE unified catch-all fallback handler for general unexpected
    // errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("UNHANDLED RUNTIME EXCEPTION DETECTED: ", ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("An unexpected system error occurred. Please try again later.")
                .data(null)
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}