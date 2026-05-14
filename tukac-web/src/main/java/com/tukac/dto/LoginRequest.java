package com.tukac.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank(message = "Email or Student ID is required")
    private String emailOrStudentId;
    
    @NotBlank(message = "Password is required")
    private String password;

    public LoginRequest() {}

    public String getEmailOrStudentId() { return emailOrStudentId; }
    public void setEmailOrStudentId(String emailOrStudentId) { this.emailOrStudentId = emailOrStudentId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
