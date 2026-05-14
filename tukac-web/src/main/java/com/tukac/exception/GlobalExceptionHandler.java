package com.tukac.exception; // Defines the package where this class belongs

import com.tukac.dto.ApiResponse; // Imports the standard response format for our API
import org.springframework.http.HttpStatus; // Imports standard HTTP status codes (404, 500, etc.)
import org.springframework.http.ResponseEntity; // Imports the wrapper for HTTP responses
import org.springframework.security.access.AccessDeniedException; // Imports the error for permission failures
import org.springframework.web.bind.MethodArgumentNotValidException; // Imports errors for wrong form information
import org.springframework.web.bind.annotation.ExceptionHandler; // Tells Spring this method handles a specific error
import org.springframework.web.bind.annotation.RestControllerAdvice; // Makes this a "Global" watcher for the entire app

import java.util.HashMap; // Imports a dictionary-style storage for field errors
import java.util.Map; // Imports the Map interface for key-value data

/**
 * Global Exception Handler for the TUKAC Web Portal.
 * This class captures all unhandled exceptions and returns them in a 
 * standardized JSON format to the frontend, fulfilling the 
 * "Exception handling & Relevant error messages" project criteria.
 */
@RestControllerAdvice // Annotation to make this class a central error trap for all controllers
public class GlobalExceptionHandler { // Start of the class definition

    @ExceptionHandler(Exception.class) // This method catches generic "Generic" errors
    public ResponseEntity<ApiResponse<String>> handleGeneralException(Exception e) { // Method signature
        e.printStackTrace(); // Prints the technical error to the server console for debugging
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR) // Returns a 500 status code
                .body(ApiResponse.error("System Error: " + e.getMessage())); // Sends the error message back to the user
    } // End of method

    @ExceptionHandler(AccessDeniedException.class) // This method catches "Access Denied" errors
    public ResponseEntity<ApiResponse<String>> handleAccessDenied(AccessDeniedException e) { // Method signature
        return ResponseEntity.status(HttpStatus.FORBIDDEN) // Returns a 403 Forbidden status
                .body(ApiResponse.error("Access Denied: You do not have permission for this action.")); // Professional message
    } // End of method

    @ExceptionHandler(MethodArgumentNotValidException.class) // This catches "Wrong Format" or "Empty Field" errors
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) { // Method signature
        Map<String, String> errors = new HashMap<>(); // Create a list to hold each field error
        ex.getBindingResult().getFieldErrors().forEach(error -> // Loop through every mistake found in the form
            errors.put(error.getField(), error.getDefaultMessage()) // Put the field name and the error message into the map
        ); // End of loop
        return ResponseEntity.status(HttpStatus.BAD_REQUEST) // Return a 400 Bad Request status
                .body(new ApiResponse<>(false, "Validation failed", errors)); // Send the specific field mistakes back to the web portal
    } // End of method

    @ExceptionHandler(RuntimeException.class) // This method catches unchecked runtime exceptions
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(RuntimeException e) { // Method signature
        return ResponseEntity.status(HttpStatus.BAD_REQUEST) // Return a 400 Bad Request status
                .body(ApiResponse.error("Operation Failed: " + e.getMessage())); // Send the operation error message
    } // End of method
} // End of class
