package com.tukac.controller;

import com.tukac.dto.ApiResponse;
import com.tukac.model.ActivityLog;
import com.tukac.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/logs")
@PreAuthorize("hasAnyRole('CHAIRPERSON', 'VICE-CHAIRPERSON')")
public class AuditLogController {

    @Autowired
    private ActivityLogService activityLogService;

    @GetMapping
    public ApiResponse<List<ActivityLog>> getLogs() {
        try {
            List<ActivityLog> logs = activityLogService.getAllLogs();
            return ApiResponse.ok(logs);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Internal Server Error: " + e.getMessage());
        }
    }
}
