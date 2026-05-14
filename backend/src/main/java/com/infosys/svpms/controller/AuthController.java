package com.infosys.svpms.controller;

import com.infosys.svpms.dto.request.LoginRequest;
import com.infosys.svpms.dto.response.ApiResponse;
import com.infosys.svpms.dto.response.LoginResponse;
import com.infosys.svpms.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login - returns JWT token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpReq) {
        String ip = httpReq.getRemoteAddr();
        LoginResponse resp = authService.login(req, ip);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", resp));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout (client should discard token)")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserDetails ud) {
        if (ud != null) authService.logout(ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset OTP (printed to console)")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestParam String email) {
        String message = authService.requestPasswordReset(email);
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP before password reset")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @RequestParam String email,
            @RequestParam String otp) {
        authService.verifyOtp(email, otp);
        return ResponseEntity.ok(ApiResponse.ok("OTP verified successfully"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using OTP")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword) {
        authService.resetPassword(email, otp, newPassword);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully"));
    }
}
