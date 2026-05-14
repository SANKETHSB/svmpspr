package com.infosys.svpms.service;

import com.infosys.svpms.dto.request.LoginRequest;
import com.infosys.svpms.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest req, String ip);
    void logout(String email);
    String requestPasswordReset(String email);
    boolean verifyOtp(String email, String otp);
    void resetPassword(String email, String otp, String newPassword);
}
