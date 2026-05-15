package com.infosys.svpms.service;

import com.infosys.svpms.dto.request.LoginRequest;
import com.infosys.svpms.dto.response.LoginResponse;
import com.infosys.svpms.entity.User;
import com.infosys.svpms.entity.Vendor;
import com.infosys.svpms.exception.BusinessException;
import com.infosys.svpms.exception.ResourceNotFoundException;
import com.infosys.svpms.repository.UserRepository;
import com.infosys.svpms.repository.VendorRepository;
import com.infosys.svpms.security.JwtUtil;
import com.infosys.svpms.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserRepository userRepo;

    @Mock
    private VendorRepository vendorRepo;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private Vendor testVendor;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .role(User.Role.ADMIN)
                .active(true)
                .locked(false)
                .failedLoginCount(0)
                .build();

        testVendor = Vendor.builder()
                .id(1L)
                .companyName("Test Company")
                .email("vendor@example.com")
                .password("encodedPassword")
                .status(Vendor.VendorStatus.APPROVED)
                .locked(false)
                .failedLoginCount(0)
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void testLoginUser_Success() {
        // Arrange
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(vendorRepo.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mock(UserDetails.class));
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("test-token");

        // Act
        LoginResponse response = authService.login(loginRequest, "127.0.0.1");

        // Assert
        assertNotNull(response);
        assertEquals("test-token", response.getAccessToken());
        assertEquals("Test User", response.getName());
        assertEquals("USER", response.getRole());
        verify(userRepo).save(any(User.class));
        verify(auditService).log(anyLong(), eq("USER"), anyString(), eq("LOGIN"), 
                eq("User"), anyLong(), anyString());
    }

    @Test
    void testLoginVendor_Success() {
        // Arrange
        loginRequest.setEmail("vendor@example.com");
        when(userRepo.findByEmail("vendor@example.com")).thenReturn(Optional.empty());
        when(vendorRepo.findByEmail("vendor@example.com")).thenReturn(Optional.of(testVendor));
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mock(UserDetails.class));
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("test-token");

        // Act
        LoginResponse response = authService.login(loginRequest, "127.0.0.1");

        // Assert
        assertNotNull(response);
        assertEquals("test-token", response.getAccessToken());
        assertEquals("Test Company", response.getName());
        assertEquals("VENDOR", response.getRole());
        verify(vendorRepo).save(any(Vendor.class));
        verify(auditService).log(anyLong(), eq("VENDOR"), anyString(), eq("LOGIN"), 
                eq("Vendor"), anyLong(), anyString());
    }

    @Test
    void testLogin_InvalidCredentials() {
        // Arrange
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(vendorRepo.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(loginRequest, "127.0.0.1");
        });
        verify(userRepo).save(any(User.class)); // Failed attempt recorded
    }

    @Test
    void testLogin_AccountLocked() {
        // Arrange
        testUser.setLocked(true);
        testUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(vendorRepo.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(LockedException.class, () -> {
            authService.login(loginRequest, "127.0.0.1");
        });
    }

    @Test
    void testLogin_UnknownEmail() {
        // Arrange
        when(userRepo.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        when(vendorRepo.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        loginRequest.setEmail("unknown@example.com");

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(loginRequest, "127.0.0.1");
        });
        verify(auditService).log(eq(0L), eq("ANONYMOUS"), anyString(), eq("LOGIN_FAILED"), 
                eq("Auth"), eq(0L), isNull(), isNull(), anyString());
    }

    @Test
    void testLogout_User() {
        // Arrange
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        authService.logout("test@example.com");

        // Assert
        verify(auditService).log(anyLong(), eq("USER"), anyString(), eq("LOGOUT"), 
                eq("User"), anyLong(), anyString());
    }

    @Test
    void testLogout_Vendor() {
        // Arrange
        when(userRepo.findByEmail("vendor@example.com")).thenReturn(Optional.empty());
        when(vendorRepo.findByEmail("vendor@example.com")).thenReturn(Optional.of(testVendor));

        // Act
        authService.logout("vendor@example.com");

        // Assert
        verify(auditService).log(anyLong(), eq("VENDOR"), anyString(), eq("LOGOUT"), 
                eq("Vendor"), anyLong(), anyString());
    }

    @Test
    void testRequestPasswordReset_User_Success() {
        // Arrange
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(vendorRepo.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // Act
        String result = authService.requestPasswordReset("test@example.com");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("OTP"));
        verify(auditService).log(anyLong(), eq("USER"), anyString(), eq("PASSWORD_RESET_REQUEST"), 
                eq("User"), anyLong(), anyString());
    }

    @Test
    void testRequestPasswordReset_Vendor_Success() {
        // Arrange
        when(userRepo.findByEmail("vendor@example.com")).thenReturn(Optional.empty());
        when(vendorRepo.findByEmail("vendor@example.com")).thenReturn(Optional.of(testVendor));

        // Act
        String result = authService.requestPasswordReset("vendor@example.com");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("OTP"));
        verify(auditService).log(anyLong(), eq("VENDOR"), anyString(), eq("PASSWORD_RESET_REQUEST"), 
                eq("Vendor"), anyLong(), anyString());
    }

    @Test
    void testRequestPasswordReset_UnknownEmail() {
        // Arrange
        when(userRepo.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        when(vendorRepo.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            authService.requestPasswordReset("unknown@example.com");
        });
    }

    @Test
    void testVerifyOtp_InvalidOtp() {
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            authService.verifyOtp("test@example.com", "123456");
        });
    }

    @Test
    void testResetPassword_ShortPassword() {
        // Arrange
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        // First request OTP
        authService.requestPasswordReset("test@example.com");

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            authService.resetPassword("test@example.com", "123456", "short");
        });
    }

    @Test
    void testLogin_MultipleFailedAttempts_LocksAccount() {
        // Arrange
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(vendorRepo.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid"));

        // Act - Simulate 5 failed attempts
        for (int i = 0; i < 5; i++) {
            try {
                authService.login(loginRequest, "127.0.0.1");
            } catch (BadCredentialsException e) {
                // Expected
            }
        }

        // Assert
        assertTrue(testUser.isLocked());
        assertNotNull(testUser.getLockedUntil());
        assertEquals(5, testUser.getFailedLoginCount());
    }
}
