package com.infosys.svpms.controller;

import com.infosys.svpms.dto.request.NotificationPreferenceRequest;
import com.infosys.svpms.dto.response.ApiResponse;
import com.infosys.svpms.dto.response.NotificationPreferenceResponse;
import com.infosys.svpms.entity.Notification;
import com.infosys.svpms.repository.UserRepository;
import com.infosys.svpms.repository.VendorRepository;
import com.infosys.svpms.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * US 13 AC #10: Notification Preferences Controller
 * 
 * Provides REST API endpoints for managing user notification preferences.
 * Users can configure which notification types they want to receive and
 * through which channels (in-app, email).
 */
@RestController
@RequestMapping("/notifications/preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Preferences", description = "Manage notification preferences")
public class NotificationPreferencesController {

    private final NotificationService notificationService;
    private final UserRepository userRepo;
    private final VendorRepository vendorRepo;

    /**
     * US 13 AC #10: Get user's notification preferences
     * 
     * Returns preferences for all notification types with in-app and email toggles.
     * If no preferences exist, returns defaults (all enabled).
     */
    @GetMapping
    @Operation(summary = "Get notification preferences", 
               description = "Get current user's notification preferences for all notification types")
    public ResponseEntity<ApiResponse<List<NotificationPreferenceResponse>>> getPreferences(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        var info = resolveActor(userDetails.getUsername());
        Long userId = (Long) info[0];
        String userType = (String) info[1];
        
        log.info("[US 13 AC #10] GET /notifications/preferences - User: {}", userId);
        
        List<NotificationPreferenceResponse> preferences = notificationService.getPreferences(userId, userType);
        
        return ResponseEntity.ok(ApiResponse.ok("Notification preferences retrieved successfully", preferences));
    }

    /**
     * US 13 AC #10: Update notification preferences
     * 
     * Updates preferences for one or more notification types.
     * Each preference includes in-app and email enabled flags.
     */
    @PutMapping
    @Operation(summary = "Update notification preferences",
               description = "Update notification preferences for specific notification types")
    public ResponseEntity<ApiResponse<String>> updatePreferences(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody List<NotificationPreferenceRequest> requests) {
        
        var info = resolveActor(userDetails.getUsername());
        Long userId = (Long) info[0];
        String userType = (String) info[1];
        
        log.info("[US 13 AC #10] PUT /notifications/preferences - User: {}, Count: {}", 
            userId, requests.size());
        
        // Convert request list to map format expected by service
        Map<Notification.NotificationType, Map<String, Boolean>> preferences = new HashMap<>();
        for (NotificationPreferenceRequest req : requests) {
            Map<String, Boolean> settings = new HashMap<>();
            settings.put("inApp", req.getInAppEnabled());
            settings.put("email", req.getEmailEnabled());
            preferences.put(req.getNotificationType(), settings);
        }
        
        notificationService.updatePreferences(userId, userType, preferences);
        
        return ResponseEntity.ok(ApiResponse.ok("Notification preferences updated successfully"));
    }

    /**
     * US 13 AC #10: Reset preferences to defaults
     * 
     * Deletes all custom preferences and reverts to defaults (all enabled).
     */
    @PostMapping("/reset")
    @Operation(summary = "Reset notification preferences",
               description = "Reset all notification preferences to defaults (all enabled)")
    public ResponseEntity<ApiResponse<String>> resetPreferences(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        var info = resolveActor(userDetails.getUsername());
        Long userId = (Long) info[0];
        String userType = (String) info[1];
        
        log.info("[US 13 AC #10] POST /notifications/preferences/reset - User: {}", userId);
        
        notificationService.resetPreferences(userId, userType);
        
        return ResponseEntity.ok(ApiResponse.ok("Notification preferences reset to defaults"));
    }

    /**
     * Resolve actor (user or vendor) from email
     */
    private Object[] resolveActor(String email) {
        var u = userRepo.findByEmail(email);
        if (u.isPresent()) return new Object[]{u.get().getId(), "USER"};
        var v = vendorRepo.findByEmail(email);
        if (v.isPresent()) return new Object[]{v.get().getId(), "VENDOR"};
        return new Object[]{0L, "USER"};
    }
}
