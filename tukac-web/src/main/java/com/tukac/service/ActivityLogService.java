package com.tukac.service;

import com.tukac.model.ActivityLog;
import com.tukac.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private HttpServletRequest request;

    public void log(String action, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        String userName = "Anonymous";

        if (auth != null && auth.isAuthenticated()) {
            if (auth.getPrincipal() instanceof UserDetails) {
                userName = ((UserDetails) auth.getPrincipal()).getUsername();
            } else if (auth.getPrincipal() instanceof String) {
                userName = (String) auth.getPrincipal();
            }
            
            if (auth.getCredentials() instanceof Long) {
                userId = (Long) auth.getCredentials();
            }
        }

        String ip = request.getRemoteAddr();
        ActivityLog log = new ActivityLog(userId, userName, action, details, ip);
        activityLogRepository.save(log);
    }

    public void logActivity(String email, String action, String details) {
        String ip = request.getRemoteAddr();
        ActivityLog log = new ActivityLog(null, email, action, details, ip);
        activityLogRepository.save(log);
    }

    public java.util.List<ActivityLog> getAllLogs() {
        return activityLogRepository.findAllByOrderByTimestampDesc();
    }
}
