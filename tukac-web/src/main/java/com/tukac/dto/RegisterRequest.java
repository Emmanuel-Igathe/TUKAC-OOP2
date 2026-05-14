package com.tukac.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Registration.
 * Includes Jakarta Validation annotations to ensure data integrity.
 */
public class RegisterRequest {
    @NotBlank(message = "Full name is required")
    private String name;

    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    private String contact;

    // Disability fields
    private boolean hasDisability;
    private String disabilityType;
    private String ncpwdNumber;

    // Passport photo (base64 data URL)
    @NotBlank(message = "Passport photo is required")
    private String passportPhoto;

    public RegisterRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public boolean isHasDisability() { return hasDisability; }
    public void setHasDisability(boolean hasDisability) { this.hasDisability = hasDisability; }

    public String getDisabilityType() { return disabilityType; }
    public void setDisabilityType(String disabilityType) { this.disabilityType = disabilityType; }

    public String getNcpwdNumber() { return ncpwdNumber; }
    public void setNcpwdNumber(String ncpwdNumber) { this.ncpwdNumber = ncpwdNumber; }

    public String getPassportPhoto() { return passportPhoto; }
    public void setPassportPhoto(String passportPhoto) { this.passportPhoto = passportPhoto; }
}
