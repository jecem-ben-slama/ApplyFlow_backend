package com.applyflow.tracker_api.config;

import com.applyflow.tracker_api.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.NoSuchElementException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Catch our targeted business logic validation errors
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 2. Catch-all fallback for any unexpected system exceptions (NullPointers,
    // Database errors, etc.)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        // Log the real error to the server console for debugging
        ex.printStackTrace();

        ApiResponse<Void> response = ApiResponse.error("An unexpected system error occurred. Please try again later.");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    // Add this import if needed


@ExceptionHandler(NoSuchElementException.class)
public ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException ex) {
    return new ResponseEntity<>(ApiResponse.error("Requested item not found: " + ex.getMessage()), HttpStatus.NOT_FOUND);
}

@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ApiResponse<Void>> handleDatabaseConstraints(DataIntegrityViolationException ex) {
    return new ResponseEntity<>(ApiResponse.error("Database constraint violation. Ensure foreign keys are correct."), HttpStatus.CONFLICT);
}
}