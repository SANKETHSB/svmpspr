import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useAuth } from "../../context/AuthContext";
import { authAPI, vendorAPI } from "../../services/api";
import "../../styles/AuthPage.css";

const AuthPage: React.FC = () => {
  const [mode, setMode] = useState<"login" | "signup" | "forgot-password">(
    "login",
  );
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const { login, user } = useAuth();

  useEffect(() => {
    // If user is already authenticated, redirect to dashboard
    if (user) {
      navigate("/dashboard");
    }
  }, [user, navigate]);

  // Login states
  const [email, setEmail] = useState("");
  const [emailError, setEmailError] = useState("");
  const [password, setPassword] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  // Signup states
  const [signupCompanyName, setSignupCompanyName] = useState("");
  const [signupCompanyNameError, setSignupCompanyNameError] = useState("");
  const [signupContactPerson, setSignupContactPerson] = useState("");
  const [signupEmail, setSignupEmail] = useState("");
  const [signupEmailError, setSignupEmailError] = useState("");
  const [signupPassword, setSignupPassword] = useState("");
  const [signupPasswordError, setSignupPasswordError] = useState("");
  const [signupConfirmPassword, setSignupConfirmPassword] = useState("");
  const [signupConfirmPasswordError, setSignupConfirmPasswordError] =
    useState("");
  const [signupGstNumber, setSignupGstNumber] = useState("");
  const [signupGstNumberError, setSignupGstNumberError] = useState("");
  const [signupRegistrationId, setSignupRegistrationId] = useState("");
  const [signupRegistrationIdError, setSignupRegistrationIdError] =
    useState("");
  const [signupPhone, setSignupPhone] = useState("");
  const [signupAddress, setSignupAddress] = useState("");
  const [showSignupPassword, setShowSignupPassword] = useState(false);
  const [showSignupConfirmPassword, setShowSignupConfirmPassword] =
    useState(false);

  // Forgot password states
  const [forgotEmail, setForgotEmail] = useState("");
  const [forgotEmailError, setForgotEmailError] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [otpVerified, setOtpVerified] = useState(false);
  const [otp, setOtp] = useState("");
  const [otpError, setOtpError] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordError, setNewPasswordError] = useState("");
  const [confirmNewPassword, setConfirmNewPassword] = useState("");
  const [confirmNewPasswordError, setConfirmNewPasswordError] = useState("");
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmNewPassword, setShowConfirmNewPassword] = useState(false);

  // Email validation
  const isValidEmail = (email: string): boolean => {
    return /\S+@\S+\.\S+/.test(email);
  };

  // Password validation
  const isValidPassword = (password: string): boolean => {
    return password.length >= 8;
  };

  // Login submission
  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    let hasError = false;

    // Validate email
    if (!email.trim()) {
      setEmailError("Email is required");
      hasError = true;
    } else if (!isValidEmail(email)) {
      setEmailError("Invalid email format");
      hasError = true;
    } else {
      setEmailError("");
    }

    // Validate password
    if (!password) {
      setPasswordError("Password is required");
      hasError = true;
    } else {
      setPasswordError("");
    }

    if (hasError) return;

    setIsLoading(true);
    try {
      const res = await authAPI.login({ email, password });
      const data = res.data.data;
      login({ ...data, accessToken: data.accessToken });
      toast.success(`Welcome back, ${data.name}!`);
      navigate("/dashboard");
    } catch (err: any) {
      const errorMessage =
        err.response?.data?.message || "Login failed. Check your credentials.";
      toast.error(errorMessage);
      setPasswordError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  // Signup submission
  const handleSignupSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    let hasError = false;

    // Validate company name
    if (!signupCompanyName.trim()) {
      setSignupCompanyNameError("Company name is required");
      hasError = true;
    } else {
      setSignupCompanyNameError("");
    }

    // Validate email
    if (!signupEmail.trim()) {
      setSignupEmailError("Email is required");
      hasError = true;
    } else if (!isValidEmail(signupEmail)) {
      setSignupEmailError("Invalid email format");
      hasError = true;
    } else {
      setSignupEmailError("");
    }

    // Validate password
    if (!signupPassword) {
      setSignupPasswordError("Password is required");
      hasError = true;
    } else if (!isValidPassword(signupPassword)) {
      setSignupPasswordError("Password must be at least 8 characters");
      hasError = true;
    } else {
      setSignupPasswordError("");
    }

    // Validate confirm password
    if (!signupConfirmPassword) {
      setSignupConfirmPasswordError("Please confirm your password");
      hasError = true;
    } else if (signupPassword !== signupConfirmPassword) {
      setSignupConfirmPasswordError("Passwords do not match");
      hasError = true;
    } else {
      setSignupConfirmPasswordError("");
    }

    // Validate GST number
    if (!signupGstNumber.trim()) {
      setSignupGstNumberError("GST number is required");
      hasError = true;
    } else if (signupGstNumber.length !== 15) {
      setSignupGstNumberError("GST number must be exactly 15 characters");
      hasError = true;
    } else {
      setSignupGstNumberError("");
    }

    // Validate registration ID
    if (!signupRegistrationId.trim()) {
      setSignupRegistrationIdError("Registration ID is required");
      hasError = true;
    } else {
      setSignupRegistrationIdError("");
    }

    if (hasError) return;

    const payload = {
      companyName: signupCompanyName,
      email: signupEmail,
      password: signupPassword,
      gstNumber: signupGstNumber,
      registrationId: signupRegistrationId,
      phone: signupPhone || undefined,
      address: signupAddress || undefined,
      contactPerson: signupContactPerson || undefined,
    };

    console.log("Sending registration payload:", payload);

    setIsLoading(true);
    try {
      const response = await vendorAPI.register(payload);
      console.log("Registration response:", response);

      // Extract verification link from response if present
      const message = response.data.message;
      console.log("=".repeat(60));
      console.log("VENDOR REGISTRATION SUCCESSFUL");
      console.log("Company:", signupCompanyName);
      console.log("Email:", signupEmail);
      console.log("Status: PENDING_APPROVAL");
      console.log("Note: Check backend console for email verification link");
      console.log("=".repeat(60));

      toast.success("Registration submitted! Await admin approval to login.");
      setMode("login");
      // Clear signup form
      setSignupCompanyName("");
      setSignupContactPerson("");
      setSignupEmail("");
      setSignupPassword("");
      setSignupConfirmPassword("");
      setSignupGstNumber("");
      setSignupRegistrationId("");
      setSignupPhone("");
      setSignupAddress("");
    } catch (err: any) {
      console.error("Registration error:", err);
      console.error("Error response:", err.response?.data);

      // Handle validation errors from backend
      if (err.response?.data?.errors) {
        const errors = err.response.data.errors;
        if (errors.companyName) setSignupCompanyNameError(errors.companyName);
        if (errors.email) setSignupEmailError(errors.email);
        if (errors.password) setSignupPasswordError(errors.password);
        if (errors.gstNumber) setSignupGstNumberError(errors.gstNumber);
        if (errors.registrationId)
          setSignupRegistrationIdError(errors.registrationId);
        toast.error(err.response.data.message || "Validation failed");
      } else {
        const errorMessage =
          err.response?.data?.message ||
          "Registration failed. Please try again.";
        toast.error(errorMessage);
      }
    } finally {
      setIsLoading(false);
    }
  };

  // Request OTP for password reset
  const handleRequestOtp = async (e: React.FormEvent) => {
    e.preventDefault();

    // Validate email
    if (!forgotEmail.trim()) {
      setForgotEmailError("Email is required");
      return;
    } else if (!isValidEmail(forgotEmail)) {
      setForgotEmailError("Invalid email format");
      return;
    } else {
      setForgotEmailError("");
    }

    setIsLoading(true);
    try {
      const response = await authAPI.forgotPassword(forgotEmail);
      const message = response.data.message || "OTP sent!";

      // Extract OTP from message if present
      const otpMatch = message.match(/OTP=(\d{6})/);
      if (otpMatch) {
        const otpCode = otpMatch[1];
        console.log("=".repeat(60));
        console.log("PASSWORD RESET OTP FOR:", forgotEmail);
        console.log("OTP CODE:", otpCode);
        console.log("=".repeat(60));
        toast.success(`OTP sent! Check browser console. OTP: ${otpCode}`);
      } else {
        console.log("OTP Response:", message);
        toast.success(message);
      }

      setOtpSent(true);
    } catch (err: any) {
      const errorMessage =
        err.response?.data?.message || "Failed to send OTP. Please try again.";
      toast.error(errorMessage);
      setForgotEmailError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  // Verify OTP
  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();

    // Validate OTP
    if (!otp.trim()) {
      setOtpError("OTP is required");
      return;
    } else if (otp.length !== 6) {
      setOtpError("OTP must be 6 digits");
      return;
    } else {
      setOtpError("");
    }

    setIsLoading(true);
    try {
      await authAPI.verifyOtp(forgotEmail, otp);
      toast.success("OTP verified! Now set your new password.");
      setOtpVerified(true);
    } catch (err: any) {
      const errorMessage =
        err.response?.data?.message || "Invalid OTP. Please try again.";
      toast.error(errorMessage);
      setOtpError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  // Reset password with OTP
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();

    let hasError = false;

    // Validate new password
    if (!newPassword) {
      setNewPasswordError("Password is required");
      hasError = true;
    } else if (!isValidPassword(newPassword)) {
      setNewPasswordError("Password must be at least 8 characters");
      hasError = true;
    } else {
      setNewPasswordError("");
    }

    // Validate confirm password
    if (!confirmNewPassword) {
      setConfirmNewPasswordError("Please confirm your password");
      hasError = true;
    } else if (newPassword !== confirmNewPassword) {
      setConfirmNewPasswordError("Passwords do not match");
      hasError = true;
    } else {
      setConfirmNewPasswordError("");
    }

    if (hasError) return;

    setIsLoading(true);
    try {
      await authAPI.resetPassword(forgotEmail, otp, newPassword);
      toast.success("Password reset successfully! You can now login.");
      // Reset form and switch to login
      setMode("login");
      setForgotEmail("");
      setOtp("");
      setNewPassword("");
      setConfirmNewPassword("");
      setOtpSent(false);
      setOtpVerified(false);
    } catch (err: any) {
      const errorMessage =
        err.response?.data?.message ||
        "Failed to reset password. Please try again.";
      toast.error(errorMessage);
      setNewPasswordError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className={`auth-container auth-mode-${mode}`}>
      <div className="auth-wrapper">
        {/* Left Panel - Gradient Card */}
        <div className="left-panel">
          <div className="gradient-card">
            <div className="card-content">
              <div className="logo">
                <div className="logo-icon">
                  <i className="bi bi-building-check"></i>
                </div>
                <span>Smart Vendor PMS</span>
              </div>

              <h1 className="main-heading">
                {mode === "signup"
                  ? "Get Started with Us"
                  : mode === "forgot-password"
                    ? "Reset Your Password"
                    : "Welcome Back"}
              </h1>

              <p className="description">
                {mode === "signup" ? (
                  <>
                    Complete these easy steps to register
                    <br />
                    your vendor account.
                  </>
                ) : mode === "forgot-password" ? (
                  <>
                    Enter your email to receive an OTP
                    <br />
                    and reset your password.
                  </>
                ) : (
                  <>
                    Sign in to your account to
                    <br />
                    continue where you left off.
                  </>
                )}
              </p>

              <div className="steps">
                {mode === "signup" ? (
                  <>
                    <button className="step-button active">
                      <span className="step-number">1</span>
                      <span className="step-text">Enter your details</span>
                    </button>
                    <button className="step-button" disabled>
                      <span className="step-number">2</span>
                      <span className="step-text">Await approval</span>
                    </button>
                    <button className="step-button" disabled>
                      <span className="step-number">3</span>
                      <span className="step-text">Access your account</span>
                    </button>
                  </>
                ) : mode === "forgot-password" ? (
                  <>
                    <button
                      className={`step-button ${!otpSent ? "active" : ""}`}
                    >
                      <span className="step-number">1</span>
                      <span className="step-text">Request OTP</span>
                    </button>
                    <button
                      className={`step-button ${otpSent && !otpVerified ? "active" : ""}`}
                      disabled={!otpSent}
                    >
                      <span className="step-number">2</span>
                      <span className="step-text">Verify OTP</span>
                    </button>
                    <button
                      className={`step-button ${otpVerified ? "active" : ""}`}
                      disabled={!otpVerified}
                    >
                      <span className="step-number">3</span>
                      <span className="step-text">Reset password</span>
                    </button>
                  </>
                ) : (
                  <>
                    <button className="step-button active">
                      <span className="step-number">1</span>
                      <span className="step-text">Enter your credentials</span>
                    </button>
                    <button className="step-button" disabled>
                      <span className="step-number">2</span>
                      <span className="step-text">Access your account</span>
                    </button>
                  </>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Right Panel - Form */}
        <div className="right-panel">
          <div className="form-container">
            {mode === "signup" ? (
              <>
                <h2 className="form-title">Vendor Registration</h2>
                <p className="form-subtitle">
                  Enter your details to create your vendor account.
                </p>

                <form className="auth-form" onSubmit={handleSignupSubmit}>
                  {/* Company Name & Contact Person */}
                  <div
                    style={{
                      display: "flex",
                      gap: "10px",
                      marginBottom: "16px",
                    }}
                  >
                    <div
                      className="form-group"
                      style={{ marginBottom: 0, flex: 1 }}
                    >
                      <label htmlFor="signup-company-name">Company Name</label>
                      <input
                        type="text"
                        id="signup-company-name"
                        placeholder="Tech Solutions Inc."
                        value={signupCompanyName}
                        onChange={(e) => {
                          setSignupCompanyName(e.target.value);
                          if (signupCompanyNameError)
                            setSignupCompanyNameError("");
                        }}
                        disabled={isLoading}
                        className={signupCompanyNameError ? "input-error" : ""}
                      />
                      {signupCompanyNameError && (
                        <p className="error-hint">{signupCompanyNameError}</p>
                      )}
                    </div>

                    <div
                      className="form-group"
                      style={{ marginBottom: 0, flex: 1 }}
                    >
                      <label htmlFor="signup-contact-person">
                        Contact Person{" "}
                        <span
                          style={{
                            color: "#a5a1d4",
                            fontWeight: "normal",
                            fontSize: "0.85em",
                          }}
                        >
                          (Optional)
                        </span>
                      </label>
                      <input
                        type="text"
                        id="signup-contact-person"
                        placeholder="John Doe"
                        value={signupContactPerson}
                        onChange={(e) => setSignupContactPerson(e.target.value)}
                        disabled={isLoading}
                      />
                    </div>
                  </div>

                  {/* Email */}
                  <div className="form-group">
                    <label htmlFor="signup-email">Email Address</label>
                    <input
                      type="email"
                      id="signup-email"
                      placeholder="you@company.com"
                      value={signupEmail}
                      onChange={(e) => {
                        setSignupEmail(e.target.value);
                        if (signupEmailError) setSignupEmailError("");
                      }}
                      disabled={isLoading}
                      className={signupEmailError ? "input-error" : ""}
                    />
                    {signupEmailError && (
                      <p className="error-hint">{signupEmailError}</p>
                    )}
                  </div>

                  {/* GST Number & Registration ID */}
                  <div
                    style={{
                      display: "flex",
                      gap: "10px",
                      marginBottom: "16px",
                    }}
                  >
                    <div
                      className="form-group"
                      style={{ marginBottom: 0, flex: 1 }}
                    >
                      <label htmlFor="signup-gst">
                        GST Number (exactly 15 chars)
                      </label>
                      <input
                        type="text"
                        id="signup-gst"
                        placeholder="22AAAAA0000A1Z5"
                        value={signupGstNumber}
                        onChange={(e) => {
                          setSignupGstNumber(e.target.value.toUpperCase());
                          if (signupGstNumberError) setSignupGstNumberError("");
                        }}
                        disabled={isLoading}
                        maxLength={15}
                        className={signupGstNumberError ? "input-error" : ""}
                      />
                      <small style={{ color: "#a5a1d4", fontSize: "0.8rem" }}>
                        {signupGstNumber.length}/15 characters
                      </small>
                      {signupGstNumberError && (
                        <p className="error-hint">{signupGstNumberError}</p>
                      )}
                    </div>

                    <div
                      className="form-group"
                      style={{ marginBottom: 0, flex: 1 }}
                    >
                      <label htmlFor="signup-reg-id">Registration ID</label>
                      <input
                        type="text"
                        id="signup-reg-id"
                        placeholder="REG123456"
                        value={signupRegistrationId}
                        onChange={(e) => {
                          setSignupRegistrationId(e.target.value);
                          if (signupRegistrationIdError)
                            setSignupRegistrationIdError("");
                        }}
                        disabled={isLoading}
                        className={
                          signupRegistrationIdError ? "input-error" : ""
                        }
                      />
                      {signupRegistrationIdError && (
                        <p className="error-hint">
                          {signupRegistrationIdError}
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Phone */}
                  <div className="form-group">
                    <label htmlFor="signup-phone">
                      Phone Number{" "}
                      <span
                        style={{
                          color: "#a5a1d4",
                          fontWeight: "normal",
                          fontSize: "0.85em",
                        }}
                      >
                        (Optional)
                      </span>
                    </label>
                    <input
                      type="tel"
                      id="signup-phone"
                      placeholder="+91 98765 43210"
                      value={signupPhone}
                      onChange={(e) => setSignupPhone(e.target.value)}
                      disabled={isLoading}
                    />
                  </div>

                  {/* Address */}
                  <div className="form-group">
                    <label htmlFor="signup-address">
                      Address{" "}
                      <span
                        style={{
                          color: "#a5a1d4",
                          fontWeight: "normal",
                          fontSize: "0.85em",
                        }}
                      >
                        (Optional)
                      </span>
                    </label>
                    <textarea
                      id="signup-address"
                      placeholder="Full business address"
                      value={signupAddress}
                      onChange={(e) => setSignupAddress(e.target.value)}
                      disabled={isLoading}
                      rows={2}
                      style={{
                        width: "100%",
                        padding: "10px 14px",
                        border: "1px solid var(--input-border)",
                        borderRadius: "8px",
                        fontSize: "0.95rem",
                        fontFamily: "inherit",
                        resize: "vertical",
                      }}
                    />
                  </div>

                  {/* Password & Confirm Password */}
                  <div
                    style={{
                      display: "flex",
                      gap: "10px",
                      marginBottom: "16px",
                    }}
                  >
                    <div
                      className="form-group"
                      style={{ marginBottom: 0, flex: 1 }}
                    >
                      <label htmlFor="signup-password">
                        Password (min 8 chars)
                      </label>
                      <div className="password-input-wrapper">
                        <input
                          type={showSignupPassword ? "text" : "password"}
                          id="signup-password"
                          placeholder="Enter a strong password"
                          value={signupPassword}
                          onChange={(e) => {
                            setSignupPassword(e.target.value);
                            if (signupPasswordError) setSignupPasswordError("");
                          }}
                          disabled={isLoading}
                          className={signupPasswordError ? "input-error" : ""}
                        />
                        <button
                          type="button"
                          className="password-toggle"
                          onClick={() =>
                            setShowSignupPassword(!showSignupPassword)
                          }
                          tabIndex={-1}
                        >
                          <i
                            className={`bi bi-eye${showSignupPassword ? "-slash" : ""}`}
                          ></i>
                        </button>
                      </div>
                      {signupPasswordError && (
                        <p className="error-hint">{signupPasswordError}</p>
                      )}
                    </div>

                    <div
                      className="form-group"
                      style={{ marginBottom: 0, flex: 1 }}
                    >
                      <label htmlFor="signup-confirm-password">
                        Confirm Password
                      </label>
                      <div className="password-input-wrapper">
                        <input
                          type={showSignupConfirmPassword ? "text" : "password"}
                          id="signup-confirm-password"
                          placeholder="Re-enter password"
                          value={signupConfirmPassword}
                          onChange={(e) => {
                            setSignupConfirmPassword(e.target.value);
                            if (signupConfirmPasswordError)
                              setSignupConfirmPasswordError("");
                          }}
                          disabled={isLoading}
                          className={
                            signupConfirmPasswordError ? "input-error" : ""
                          }
                        />
                        <button
                          type="button"
                          className="password-toggle"
                          onClick={() =>
                            setShowSignupConfirmPassword(
                              !showSignupConfirmPassword,
                            )
                          }
                          tabIndex={-1}
                        >
                          <i
                            className={`bi bi-eye${showSignupConfirmPassword ? "-slash" : ""}`}
                          ></i>
                        </button>
                      </div>
                      {signupConfirmPasswordError && (
                        <p className="error-hint">
                          {signupConfirmPasswordError}
                        </p>
                      )}
                    </div>
                  </div>

                  <button
                    type="submit"
                    className="submit-button"
                    disabled={
                      isLoading ||
                      !signupEmail ||
                      !signupPassword ||
                      !signupCompanyName
                    }
                  >
                    {isLoading ? "Registering..." : "Register Vendor"}
                  </button>
                </form>

                <p className="login-link">
                  Already have an account?{" "}
                  <a
                    href="#"
                    onClick={(e) => {
                      e.preventDefault();
                      setMode("login");
                    }}
                  >
                    Log in
                  </a>
                </p>
              </>
            ) : mode === "forgot-password" ? (
              <>
                <h2 className="form-title">Reset Password</h2>
                <p className="form-subtitle">
                  {!otpSent
                    ? "Enter your email to receive an OTP."
                    : !otpVerified
                      ? "Enter the OTP from backend console to verify."
                      : "Set your new password."}
                </p>

                {!otpSent ? (
                  <form className="auth-form" onSubmit={handleRequestOtp}>
                    {/* Email */}
                    <div className="form-group">
                      <label htmlFor="forgot-email">Email Address</label>
                      <input
                        type="email"
                        id="forgot-email"
                        placeholder="you@company.com"
                        value={forgotEmail}
                        onChange={(e) => {
                          setForgotEmail(e.target.value);
                          if (forgotEmailError) setForgotEmailError("");
                        }}
                        disabled={isLoading}
                        className={forgotEmailError ? "input-error" : ""}
                      />
                      {forgotEmailError && (
                        <p className="error-hint">{forgotEmailError}</p>
                      )}
                    </div>

                    <button
                      type="submit"
                      className="submit-button"
                      disabled={isLoading || !forgotEmail}
                    >
                      {isLoading ? "Sending OTP..." : "Send OTP"}
                    </button>
                  </form>
                ) : !otpVerified ? (
                  <form className="auth-form" onSubmit={handleVerifyOtp}>
                    {/* OTP */}
                    <div className="form-group">
                      <label htmlFor="otp">
                        OTP Code (Check Backend Console)
                      </label>
                      <input
                        type="text"
                        id="otp"
                        placeholder="Enter 6-digit OTP"
                        value={otp}
                        onChange={(e) => {
                          setOtp(e.target.value.replace(/\D/g, ""));
                          if (otpError) setOtpError("");
                        }}
                        disabled={isLoading}
                        maxLength={6}
                        className={otpError ? "input-error" : ""}
                      />
                      {otpError && <p className="error-hint">{otpError}</p>}
                    </div>

                    <button
                      type="submit"
                      className="submit-button"
                      disabled={isLoading || !otp || otp.length !== 6}
                    >
                      {isLoading ? "Verifying..." : "Verify OTP"}
                    </button>
                  </form>
                ) : (
                  <form className="auth-form" onSubmit={handleResetPassword}>
                    {/* New Password */}
                    <div className="form-group">
                      <label htmlFor="new-password">
                        New Password (min 8 chars)
                      </label>
                      <div className="password-input-wrapper">
                        <input
                          type={showNewPassword ? "text" : "password"}
                          id="new-password"
                          placeholder="Enter new password"
                          value={newPassword}
                          onChange={(e) => {
                            setNewPassword(e.target.value);
                            if (newPasswordError) setNewPasswordError("");
                          }}
                          disabled={isLoading}
                          className={newPasswordError ? "input-error" : ""}
                        />
                        <button
                          type="button"
                          className="password-toggle"
                          onClick={() => setShowNewPassword(!showNewPassword)}
                          tabIndex={-1}
                        >
                          <i
                            className={`bi bi-eye${showNewPassword ? "-slash" : ""}`}
                          ></i>
                        </button>
                      </div>
                      {newPasswordError && (
                        <p className="error-hint">{newPasswordError}</p>
                      )}
                    </div>

                    {/* Confirm New Password */}
                    <div className="form-group">
                      <label htmlFor="confirm-new-password">
                        Confirm New Password
                      </label>
                      <div className="password-input-wrapper">
                        <input
                          type={showConfirmNewPassword ? "text" : "password"}
                          id="confirm-new-password"
                          placeholder="Re-enter new password"
                          value={confirmNewPassword}
                          onChange={(e) => {
                            setConfirmNewPassword(e.target.value);
                            if (confirmNewPasswordError)
                              setConfirmNewPasswordError("");
                          }}
                          disabled={isLoading}
                          className={
                            confirmNewPasswordError ? "input-error" : ""
                          }
                        />
                        <button
                          type="button"
                          className="password-toggle"
                          onClick={() =>
                            setShowConfirmNewPassword(!showConfirmNewPassword)
                          }
                          tabIndex={-1}
                        >
                          <i
                            className={`bi bi-eye${showConfirmNewPassword ? "-slash" : ""}`}
                          ></i>
                        </button>
                      </div>
                      {confirmNewPasswordError && (
                        <p className="error-hint">{confirmNewPasswordError}</p>
                      )}
                    </div>

                    <button
                      type="submit"
                      className="submit-button"
                      disabled={
                        isLoading || !newPassword || !confirmNewPassword
                      }
                    >
                      {isLoading ? "Resetting..." : "Reset Password"}
                    </button>
                  </form>
                )}

                <p className="login-link">
                  Remember your password?{" "}
                  <a
                    href="#"
                    onClick={(e) => {
                      e.preventDefault();
                      setMode("login");
                      setOtpSent(false);
                      setOtpVerified(false);
                      setForgotEmail("");
                      setOtp("");
                      setNewPassword("");
                      setConfirmNewPassword("");
                    }}
                  >
                    Log in
                  </a>
                </p>
              </>
            ) : (
              <>
                <h2 className="form-title">Log In</h2>
                <p className="form-subtitle">
                  Enter your credentials to get started.
                </p>

                <form className="auth-form" onSubmit={handleLoginSubmit}>
                  {/* Email */}
                  <div className="form-group">
                    <label htmlFor="email">Email Address</label>
                    <input
                      type="email"
                      id="email"
                      placeholder="you@company.com"
                      value={email}
                      onChange={(e) => {
                        setEmail(e.target.value);
                        if (emailError) setEmailError("");
                      }}
                      disabled={isLoading}
                      className={emailError ? "input-error" : ""}
                    />
                    {emailError && <p className="error-hint">{emailError}</p>}
                  </div>

                  {/* Password */}
                  <div className="form-group">
                    <label htmlFor="password">Password</label>
                    <div className="password-input-wrapper">
                      <input
                        type={showPassword ? "text" : "password"}
                        id="password"
                        placeholder="Enter your password"
                        value={password}
                        onChange={(e) => {
                          setPassword(e.target.value);
                          if (passwordError) setPasswordError("");
                        }}
                        disabled={isLoading}
                        className={passwordError ? "input-error" : ""}
                      />
                      <button
                        type="button"
                        className="password-toggle"
                        onClick={() => setShowPassword(!showPassword)}
                        tabIndex={-1}
                      >
                        <i
                          className={`bi bi-eye${showPassword ? "-slash" : ""}`}
                        ></i>
                      </button>
                    </div>
                    {passwordError && (
                      <p className="error-hint">{passwordError}</p>
                    )}
                  </div>

                  {/* Forgot Password Link */}
                  <div
                    style={{
                      textAlign: "right",
                      marginTop: "-8px",
                      marginBottom: "20px",
                    }}
                  >
                    <a
                      href="#"
                      onClick={(e) => {
                        e.preventDefault();
                        setMode("forgot-password");
                      }}
                      style={{
                        color: "#c4b5fd",
                        fontSize: "0.9rem",
                        textDecoration: "none",
                        fontWeight: "500",
                      }}
                      onMouseOver={(e) => {
                        e.currentTarget.style.textDecoration = "underline";
                      }}
                      onMouseOut={(e) => {
                        e.currentTarget.style.textDecoration = "none";
                      }}
                    >
                      Forgot Password?
                    </a>
                  </div>

                  <button
                    type="submit"
                    className="submit-button"
                    disabled={isLoading || !email || !password}
                  >
                    {isLoading ? "Logging in..." : "Log In"}
                  </button>
                </form>

                <p className="signup-link">
                  Don't have an account?{" "}
                  <a
                    href="#"
                    onClick={(e) => {
                      e.preventDefault();
                      setMode("signup");
                    }}
                  >
                    Sign up
                  </a>
                </p>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AuthPage;
