package com.tukac.controller;

import com.tukac.model.Transaction;
import com.tukac.model.User;
import com.tukac.repository.TransactionRepository;
import com.tukac.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for tracking club finances.
 * Manages income and expenses using high-precision calculations.
 */
@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    @Autowired private TransactionRepository transRepo;
    @Autowired private UserRepository userRepo;

    /**
     * REPORTING: Retrieves all transactions and calculates the balance.
     * Logic: Uses aggregate database functions (sumByType) to calculate 
     * total income and expenses efficiently.
     */
    @GetMapping
    public ResponseEntity<?> getFinances() {
        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transRepo.findAllByOrderByDateDesc());
        BigDecimal income = transRepo.sumByType("INCOME");
        BigDecimal expense = transRepo.sumByType("EXPENSE");
        response.put("totalIncome", income);
        response.put("totalExpense", expense);
        response.put("balance", income.subtract(expense));
        return ResponseEntity.ok(response);
    }

    /**
     * ADD: Records a new income or expense.
     * Security: Identifies the currently logged-in user to attribute 
     * the record for accountability.
     */
    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody Transaction transaction, @AuthenticationPrincipal UserDetails principal) {
        User user = userRepo.findByEmail(principal.getUsername()).orElseThrow();
        transaction.setCreatedBy(user);
        Transaction saved = transRepo.save(transaction);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        transRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Transaction deleted"));
    }
}
