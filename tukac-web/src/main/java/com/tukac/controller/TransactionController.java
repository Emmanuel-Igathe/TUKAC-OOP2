package com.tukac.controller; // Defines the package where this class belongs

import com.tukac.dto.ApiResponse; // Imports the standard response format for our API
import com.tukac.model.Transaction; // Imports the Transaction data model
import com.tukac.repository.TransactionRepository; // Imports the database handler for transactions
import org.springframework.beans.factory.annotation.Autowired; // Imports the tool for automatic dependency injection
import org.springframework.http.ResponseEntity; // Imports the wrapper for HTTP responses
import org.springframework.security.access.prepost.PreAuthorize; // Imports the security tool for role checking
import org.springframework.security.core.Authentication; // Imports the tool to get current user info
import org.springframework.web.bind.annotation.*; // Imports standard web routing annotations

import java.util.HashMap; // Imports a dictionary-style storage for responses
import java.util.List; // Imports the List interface for collections
import java.util.Map; // Imports the Map interface for key-value data
import java.util.Optional; // Imports a container to handle null safety

/**
 * Controller for managing club finances.
 * Handles the recording, updating, and retrieval of income and expenses.
 */
@RestController // Annotation to make this a RESTful web controller
@RequestMapping("/api/transactions") // The base URL for all endpoints in this class
public class TransactionController { // Start of the class definition

    @Autowired private TransactionRepository transactionRepository; // Injects the transaction database handler
    @Autowired private com.tukac.service.ActivityLogService activityLogService; // Injects the audit logging service

    /**
     * BROWSE/SEARCH: Retrieves financial records and calculates summary statistics.
     * Logic: Calculates total income, total expenses, and the net balance 
     * on-the-fly using Java Streams for high performance.
     */
    @GetMapping // Handles GET requests to /api/transactions
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTransactions(@RequestParam(required = false) String search) { // Method signature
        List<Transaction> transactions; // Variable to hold the list of records
        if (search != null && !search.isEmpty()) { // If the user typed something in the search box
            // Filter by description or category
            transactions = transactionRepository.findByDescriptionContainingIgnoreCaseOrCategoryContainingIgnoreCase(search, search);
        } else { // If no search query was provided
            // Retrieve all transactions ordered by date
            transactions = transactionRepository.findAllByOrderByTransactionDateDesc();
        } // End of if-else
        
        // Summation logic using Java 8 Streams
        Double balance = transactionRepository.calculateBalance();

        double income = transactions.stream() // Start streaming the transaction list
                .filter(t -> "income".equalsIgnoreCase(t.getType())) // Filter to keep only income records
                .mapToDouble(Transaction::getAmount).sum(); // Convert amounts to double and sum them up
                
        double expense = transactions.stream() // Start streaming the transaction list
                .filter(t -> "expense".equalsIgnoreCase(t.getType())) // Filter to keep only expense records
                .mapToDouble(Transaction::getAmount).sum(); // Convert amounts to double and sum them up

        Map<String, Object> result = new HashMap<>(); // Create a map to bundle all data together
        result.put("transactions", transactions); // Add the list of records to the map
        result.put("balance", balance != null ? balance : 0.0); // Add the net balance (handle null with 0.0)
        result.put("totalIncome", income); // Add the calculated total income
        result.put("totalExpense", expense); // Add the calculated total expense

        return ResponseEntity.ok(ApiResponse.ok(result)); // Return the bundle with a 200 OK status
    } // End of method

    /**
     * ADD: Records a new financial transaction.
     * Access Control: Chairperson, Vice-Chairperson, and Treasurer.
     */
    @PostMapping // Handles POST requests to create a new record
    @PreAuthorize("hasAnyRole('CHAIRPERSON','VICE-CHAIRPERSON','TREASURER')") // Security check for authorized roles
    public ResponseEntity<ApiResponse<Transaction>> createTransaction( // Method signature
            @RequestBody Transaction transaction, Authentication auth) { // Accepts the transaction data and user info
        Long userId = (Long) auth.getCredentials(); // Gets the ID of the person making the entry
        transaction.setCreatedBy(userId); // Links the transaction to that user
        Transaction saved = transactionRepository.save(transaction); // Saves the record to the database
        
        // Real-time audit log for financial accountability
        activityLogService.log("RECORD_FINANCE", "Recorded " + saved.getType() + ": " + saved.getDescription() + " (" + saved.getAmount() + ")");
        return ResponseEntity.ok(ApiResponse.ok("Transaction recorded", saved)); // Return the saved record with success message
    } // End of method

    /**
     * DELETE: Removes a transaction record.
     */
    @DeleteMapping("/{id}") // Handles DELETE requests for a specific ID
    @PreAuthorize("hasAnyRole('CHAIRPERSON','VICE-CHAIRPERSON','TREASURER')") // Security check
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long id) { // Method signature
        if (!transactionRepository.existsById(id)) return ResponseEntity.notFound().build(); // Return 404 if not found
        Transaction transaction = transactionRepository.findById(id).get(); // Get the record details for logging
        transactionRepository.deleteById(id); // Delete the record from the database
        activityLogService.log("DELETE_FINANCE", "Deleted transaction: " + transaction.getDescription()); // Audit log entry
        return ResponseEntity.ok(ApiResponse.ok("Transaction deleted", null)); // Return success message
    } // End of method

    /**
     * UPDATE/EDIT: Updates an existing transaction.
     * Useful for correcting data entry errors.
     */
    @PutMapping("/{id}") // Handles PUT requests to update a specific ID
    @PreAuthorize("hasAnyRole('CHAIRPERSON','VICE-CHAIRPERSON','TREASURER')") // Security check
    public ResponseEntity<ApiResponse<Transaction>> updateTransaction(@PathVariable Long id, @RequestBody Transaction updated) { // Method signature
        Optional<Transaction> opt = transactionRepository.findById(id); // Look for the existing record
        if (opt.isEmpty()) return ResponseEntity.notFound().build(); // Return 404 if record doesn't exist

        Transaction tx = opt.get(); // Get the existing record object
        tx.setType(updated.getType()); // Update the type (Income/Expense)
        tx.setDescription(updated.getDescription()); // Update the description
        tx.setAmount(updated.getAmount()); // Update the monetary amount
        tx.setCategory(updated.getCategory()); // Update the category
        tx.setTransactionDate(updated.getTransactionDate()); // Update the date

        Transaction saved = transactionRepository.save(tx); // Save the modified record back to the database
        activityLogService.log("UPDATE_FINANCE", "Updated transaction: " + saved.getDescription() + " (" + saved.getAmount() + ")"); // Audit log
        return ResponseEntity.ok(ApiResponse.ok("Transaction updated", saved)); // Return the updated record
    } // End of method
} // End of class
