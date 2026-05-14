package com.infosys.svpms.service.impl;

import com.infosys.svpms.dto.request.LoginRequest;
import com.infosys.svpms.dto.response.LoginResponse;
import com.infosys.svpms.entity.*;
import com.infosys.svpms.exception.BusinessException;
import com.infosys.svpms.exception.ResourceNotFoundException;
import com.infosys.svpms.repository.*;
import com.infosys.svpms.security.JwtUtil;
import com.infosys.svpms.service.AuditService;
import com.infosys.svpms.service.AuthService;
import com.infosys.svpms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service @RequiredArgsConstructor @Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService uds;
    private final UserRepository userRepo;
    private final VendorRepository vendorRepo;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private static final int MAX_ATTEMPTS = 5;
    
    // In-memory OTP storage (email -> OTP + expiry)
    private static final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();
    private static final int OTP_EXPIRY_MINUTES = 10;
    
    private static class OtpData {
        String otp;
        LocalDateTime expiry;
        OtpData(String otp, LocalDateTime expiry) {
            this.otp = otp;
            this.expiry = expiry;
        }
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest req, String ip) {
        // Check for user account first
        var userOpt = userRepo.findByEmail(req.getEmail());
        var vendorOpt = vendorRepo.findByEmail(req.getEmail());

        if (userOpt.isEmpty() && vendorOpt.isEmpty()) {
            // US 14 #8 — log failed login attempts even when the email is unknown.
            auditService.log(0L, "ANONYMOUS", req.getEmail(), "LOGIN_FAILED",
                "Auth", 0L, null, null,
                "Failed login (unknown email) from " + ip);
            throw new BadCredentialsException("Invalid email or password.");
        }

        // Pre-check lock status
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            if (u.isLocked() && u.getLockedUntil() != null && u.getLockedUntil().isAfter(LocalDateTime.now())) {
                throw new LockedException("Account locked until " + u.getLockedUntil());
            } else if (u.isLocked()) {
                u.setLocked(false);
                u.setFailedLoginCount(0);
                userRepo.save(u);
            }
        }
        if (vendorOpt.isPresent()) {
            Vendor v = vendorOpt.get();
            if (v.isLocked() && v.getLockedUntil() != null && v.getLockedUntil().isAfter(LocalDateTime.now())) {
                throw new LockedException("Account locked until " + v.getLockedUntil());
            }
        }

        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (AuthenticationException e) {
            handleFailedLogin(userOpt.orElse(null), vendorOpt.orElse(null));
            throw new BadCredentialsException("Invalid email or password.");
        }

        // Reset failed attempts on success
        userOpt.ifPresent(u -> {
            u.setFailedLoginCount(0);
            u.setLastLoginAt(LocalDateTime.now());
            userRepo.save(u);
        });
        vendorOpt.ifPresent(v -> {
            v.setFailedLoginCount(0);
            v.setLastLoginAt(LocalDateTime.now());
            vendorRepo.save(v);
        });

        UserDetails ud = uds.loadUserByUsername(req.getEmail());
        Map<String, Object> claims = new HashMap<>();

        if (userOpt.isPresent()) {
            User u = userOpt.get();
            // AC #4: Use customRoleName if assigned, otherwise enum role
            String effectiveRole = (u.getCustomRoleName() != null && !u.getCustomRoleName().isBlank())
                ? u.getCustomRoleName() : u.getRole().name();
            claims.put("role", effectiveRole);
            claims.put("actorType", "USER");
            String token = jwtUtil.generateToken(req.getEmail(), claims);
            auditService.log(u.getId(), "USER", u.getName(), "LOGIN", "User", u.getId(), "User logged in from " + ip);
            return LoginResponse.builder()
                .accessToken(token).tokenType("Bearer")
                .id(u.getId()).name(u.getName()).email(u.getEmail())
                .role("USER").roleType(effectiveRole)
                .build();
        } else {
            Vendor v = vendorOpt.get();
            claims.put("role", "VENDOR");
            claims.put("actorType", "VENDOR");
            String token = jwtUtil.generateToken(req.getEmail(), claims);
            auditService.log(v.getId(), "VENDOR", v.getCompanyName(), "LOGIN", "Vendor", v.getId(), "Vendor logged in from " + ip);
            return LoginResponse.builder()
                .accessToken(token).tokenType("Bearer")
                .id(v.getId()).name(v.getCompanyName()).email(v.getEmail())
                .role("VENDOR").roleType("VENDOR")
                .build();
        }
    }

    private void handleFailedLogin(User user, Vendor vendor) {
        if (user != null) {
            int attempts = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(attempts);
            if (attempts >= MAX_ATTEMPTS) {
                user.setLocked(true);
                user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
                log.warn("User account locked: {}", user.getEmail());
            }
            userRepo.save(user);
            // US 14 #8 — log every failed login attempt for a known user.
            auditService.log(user.getId(), "USER", user.getName(),
                attempts >= MAX_ATTEMPTS ? "LOGIN_FAILED_LOCKED" : "LOGIN_FAILED",
                "User", user.getId(), null, null,
                "Failed login attempt #" + attempts + " for " + user.getEmail());
        }
        if (vendor != null) {
            int attempts = vendor.getFailedLoginCount() + 1;
            vendor.setFailedLoginCount(attempts);
            if (attempts >= MAX_ATTEMPTS) {
                vendor.setLocked(true);
                vendor.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            }
            vendorRepo.save(vendor);
            // US 14 #8 — log every failed login attempt for a known vendor.
            auditService.log(vendor.getId(), "VENDOR", vendor.getCompanyName(),
                attempts >= MAX_ATTEMPTS ? "LOGIN_FAILED_LOCKED" : "LOGIN_FAILED",
                "Vendor", vendor.getId(), null, null,
                "Failed login attempt #" + attempts + " for " + vendor.getEmail());
        }
    }

    @Override
    public void logout(String email) {
        // JWT is stateless; client discards token. Log the event.
        userRepo.findByEmail(email).ifPresent(u ->
            auditService.log(u.getId(), "USER", u.getName(), "LOGOUT", "User", u.getId(), "User logged out"));
        vendorRepo.findByEmail(email).ifPresent(v ->
            auditService.log(v.getId(), "VENDOR", v.getCompanyName(), "LOGOUT", "Vendor", v.getId(), "Vendor logged out"));
    }

    @Override
    @Transactional
    public String requestPasswordReset(String email) {
        // Check if user or vendor exists
        var userOpt = userRepo.findByEmail(email);
        var vendorOpt = vendorRepo.findByEmail(email);
        
        if (userOpt.isEmpty() && vendorOpt.isEmpty()) {
            throw new ResourceNotFoundException("No account found with email: " + email);
        }
        
        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        
        // Store OTP
        otpStore.put(email, new OtpData(otp, expiry));
        
        // Print OTP to console (as per requirement - no email needed)
        log.info("=".repeat(60));
        log.info("PASSWORD RESET OTP FOR: {}", email);
        log.info("OTP CODE: {}", otp);
        log.info("VALID UNTIL: {}", expiry);
        log.info("=".repeat(60));
        
        // Also log to audit
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            auditService.log(u.getId(), "USER", u.getName(), "PASSWORD_RESET_REQUEST", "User", u.getId(), 
                "Password reset OTP generated");
        } else {
            Vendor v = vendorOpt.get();
            auditService.log(v.getId(), "VENDOR", v.getCompanyName(), "PASSWORD_RESET_REQUEST", "Vendor", v.getId(), 
                "Password reset OTP generated");
        }
        
        // Return OTP in message for testing (browser console)
        return "OTP sent successfully. Check console for OTP code. [TESTING: OTP=" + otp + "]";
    }

    @Override
    @Transactional
    public boolean verifyOtp(String email, String otp) {
        // Validate OTP
        OtpData otpData = otpStore.get(email);
        if (otpData == null) {
            throw new BusinessException("No OTP found for this email. Please request a new OTP.");
        }
        
        if (LocalDateTime.now().isAfter(otpData.expiry)) {
            otpStore.remove(email);
            throw new BusinessException("OTP has expired. Please request a new OTP.");
        }
        
        if (!otpData.otp.equals(otp)) {
            throw new BusinessException("Invalid OTP. Please try again.");
        }
        
        log.info("OTP verified successfully for: {}", email);
        return true;
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        // Verify OTP again for security
        verifyOtp(email, otp);
        
        // Validate password complexity (min 8 chars)
        if (newPassword.length() < 8) {
            throw new BusinessException("Password must be at least 8 characters long.");
        }
        
        // Update password
        var userOpt = userRepo.findByEmail(email);
        var vendorOpt = vendorRepo.findByEmail(email);
        
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            u.setPassword(passwordEncoder.encode(newPassword));
            u.setFailedLoginCount(0);
            u.setLocked(false);
            u.setLockedUntil(null);
            userRepo.save(u);
            auditService.log(u.getId(), "USER", u.getName(), "PASSWORD_RESET", "User", u.getId(), 
                "Password reset successfully");
            log.info("Password reset successful for user: {}", email);
        } else if (vendorOpt.isPresent()) {
            Vendor v = vendorOpt.get();
            v.setPassword(passwordEncoder.encode(newPassword));
            v.setFailedLoginCount(0);
            v.setLocked(false);
            v.setLockedUntil(null);
            vendorRepo.save(v);
            auditService.log(v.getId(), "VENDOR", v.getCompanyName(), "PASSWORD_RESET", "Vendor", v.getId(), 
                "Password reset successfully");
            log.info("Password reset successful for vendor: {}", email);
        }
        
        // Remove OTP after successful reset
        otpStore.remove(email);
    }
}
