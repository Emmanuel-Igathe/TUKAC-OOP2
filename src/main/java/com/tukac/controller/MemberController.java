package com.tukac.controller;

import com.tukac.model.User;
import com.tukac.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing club members and membership approvals.
 * This class handles the administrative workflow for student registration.
 */
@RestController
@RequestMapping("/api/members")
public class MemberController {

    @Autowired private UserRepository userRepo;

    /**
     * BROWSE: Retrieves all registered members.
     * Logic: Returns a categorized list containing both fully registered 
     * members and those whose applications are still "PENDING".
     */
    @GetMapping
    public ResponseEntity<?> getAllMembers() {
        Map<String, Object> response = new HashMap<>();
        response.put("members", userRepo.findAll());
        response.put("pending", userRepo.findByApprovalStatus("PENDING"));
        return ResponseEntity.ok(response);
    }

    /**
     * UPDATE: Approves a member's registration.
     * Business Logic: Locates the user by ID and updates their status to "APPROVED",
     * which then allows them to log in to the portal.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveMember(@PathVariable Long id) {
        userRepo.findById(id).ifPresent(u -> {
            u.setApprovalStatus("APPROVED");
            userRepo.save(u);
        });
        return ResponseEntity.ok(Map.of("message", "Member approved"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectMember(@PathVariable Long id) {
        userRepo.findById(id).ifPresent(u -> {
            u.setApprovalStatus("REJECTED");
            userRepo.save(u);
        });
        return ResponseEntity.ok(Map.of("message", "Member rejected"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMember(@PathVariable Long id) {
        userRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Member deleted"));
    }
}
