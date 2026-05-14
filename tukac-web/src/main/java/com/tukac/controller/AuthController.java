package com.tukac.controller;

import com.tukac.dto.ApiResponse;
import com.tukac.dto.AuthResponse;
import com.tukac.dto.LoginRequest;
import com.tukac.dto.RegisterRequest;
import com.tukac.model.User;
import com.tukac.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@jakarta.validation.Valid @RequestBody LoginRequest req) {
        if (req.getEmailOrStudentId() == null || req.getPassword() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email/ID and password are required."));
        }

        Optional<User> userOpt = authService.authenticate(req.getEmailOrStudentId(), req.getPassword());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid credentials. Please try again."));
        }

        User user = userOpt.get();

        if (user.getIsApproved() == 0) {
            return ResponseEntity.status(403).body(ApiResponse.error("Your account is pending admin approval."));
        }

        String token = authService.generateToken(user);
        AuthResponse authResponse = new AuthResponse(
            token, user.getName(), user.getRole(), user.getId(), user.getStudentId(), user.getEmail()
        );

        return ResponseEntity.ok(ApiResponse.ok("Login successful", authResponse));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String studentId = body.get("studentId");
        
        if (email == null || studentId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email and Student ID are required."));
        }
        
        Optional<User> userOpt = authService.authenticateReset(email, studentId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("User not found or details mismatch."));
        }
        
        // In a real app, send email. Here we just reset to default for demo.
        authService.resetPassword(userOpt.get(), "Reset@123");
        
        return ResponseEntity.ok(ApiResponse.ok("Password has been reset to: Reset@123. Please login and change it.", null));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@jakarta.validation.Valid @RequestBody RegisterRequest req) {
        if (req.getName() == null || req.getStudentId() == null || req.getEmail() == null || req.getPassword() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("All fields are required."));
        }
        if (req.getPassportPhoto() == null || req.getPassportPhoto().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Passport photo is required."));
        }
        if (authService.emailExists(req.getEmail())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email already registered."));
        }
        if (authService.studentIdExists(req.getStudentId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student ID already registered."));
        }

        authService.register(
            req.getName(), req.getStudentId(), req.getEmail(), req.getPassword(), req.getContact(),
            req.isHasDisability(), req.getDisabilityType(), req.getNcpwdNumber(), req.getPassportPhoto()
        );
        return ResponseEntity.ok(ApiResponse.ok("Registration successful! Please wait for admin approval.", null));
    }
}
