package com.tukac.service;

import com.tukac.model.User;
import com.tukac.repository.ActivityLogRepository;
import com.tukac.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.tukac.dto.ApiResponse;
import java.util.Optional;

/**
 * Core Security Service.
 * This class handles sensitive operations like password hashing, 
 * user authentication, and session token generation.
 */
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private HttpServletRequest request;

    /**
     * BCrypt is a industry-standard hashing algorithm.
     * It prevents passwords from being readable even if the database is compromised.
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * AUTHENTICATION: Verifies user credentials.
     * Logic: Supports both legacy plain-text (from the desktop app) 
     * and modern BCrypt hashes (from the web app).
     */
    public Optional<User> authenticate(String emailOrStudentId, String rawPassword) {
        Optional<User> userOpt = userRepository.findByEmailOrStudentId(emailOrStudentId, emailOrStudentId);
        if (userOpt.isEmpty()) return Optional.empty();

        User user = userOpt.get();
        String storedPassword = user.getPassword();

        boolean matches;
        // Detect if the stored password is a BCrypt hash
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
            matches = passwordEncoder.matches(rawPassword, storedPassword);
        } else {
            // Support for legacy plain-text passwords
            matches = storedPassword.equals(rawPassword);
        }

        if (matches) {
            // Trigger an audit log entry on successful login
            logActivity(user.getId(), user.getName(), "LOGIN", "User logged in via Web Portal");
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /**
     * AUDIT LOG HELPER: Standardizes how security events are logged.
     */
    public void logActivity(Long userId, String userName, String action, String details) {
        activityLogService.log(action, details);
    }

    /**
     * TOKEN GENERATION: Creates a JWT (JSON Web Token).
     * This token is sent to the browser and used for all subsequent requests,
     * making the API "stateless" and secure.
     */
    public String generateToken(User user) {
        return jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
    }

    /**
     * PASSWORD RESET: Verifies identity using Email + Student ID.
     */
    public Optional<User> authenticateReset(String email, String studentId) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent() && userOpt.get().getStudentId().equals(studentId)) {
            return userOpt;
        }
        return Optional.empty();
    }

    /**
     * PASSWORD RESET: Updates the user password with a new secure hash.
     */
    public void resetPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logActivity(user.getId(), user.getName(), "PASSWORD_RESET", "Password was reset by the user");
    }

    /**
     * REGISTRATION: Creates a new user account.
     * Logic: All new users default to "member" role and require admin approval (isApproved=0).
     * Includes handling for disability details and passport photos.
     */
    public User register(String name, String studentId, String email, String rawPassword, String contact,
                         boolean hasDisability, String disabilityType, String ncpwdNumber, String passportPhoto) {
        User user = new User();
        user.setName(name);
        user.setStudentId(studentId);
        user.setEmail(email);
        // Encrypt the password before saving
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setContact(contact);
        user.setRole("member");
        user.setIsApproved(0); // Pending Admin Approval
        user.setHasDisability(hasDisability);
        user.setDisabilityType(hasDisability ? disabilityType : null);
        user.setNcpwdNumber(hasDisability ? ncpwdNumber : null);
        user.setPassportPhoto(passportPhoto);
        
        User saved = userRepository.save(user);
        logActivity(saved.getId(), saved.getName(), "REGISTER", "New user registered: " + saved.getEmail());
        return saved;
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public boolean studentIdExists(String studentId) {
        return userRepository.findByStudentId(studentId).isPresent();
    }

    /**
     * FORGOT PASSWORD: Self-service reset functionality.
     */
    public ApiResponse<String> forgotPassword(String email, String studentId) {
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isPresent() && opt.get().getStudentId().equalsIgnoreCase(studentId)) {
            User user = opt.get();
            // Reset to a known temporary password
            user.setPassword(passwordEncoder.encode("reset123"));
            userRepository.save(user);
            
            activityLogService.logActivity(user.getEmail(), "PASSWORD_RESET_REQUEST", "User reset their own password via student ID verification.");
            
            return ApiResponse.ok("Password has been reset to 'reset123'. Please log in and change it immediately.");
        }
        return ApiResponse.error("No account found with these details.");
    }
}
