package com.infosys.svpms.service;

import com.infosys.svpms.dto.request.VendorRegisterRequest;
import com.infosys.svpms.dto.response.VendorResponse;
import com.infosys.svpms.entity.User;
import com.infosys.svpms.entity.Vendor;
import com.infosys.svpms.exception.BusinessException;
import com.infosys.svpms.exception.DuplicateException;
import com.infosys.svpms.exception.ResourceNotFoundException;
import com.infosys.svpms.repository.UserRepository;
import com.infosys.svpms.repository.VendorApprovalHistoryRepository;
import com.infosys.svpms.repository.VendorRepository;
import com.infosys.svpms.service.impl.VendorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock
    private VendorRepository vendorRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private VendorApprovalHistoryRepository historyRepo;

    @InjectMocks
    private VendorServiceImpl vendorService;

    private Vendor testVendor;
    private User adminUser;
    private VendorRegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testVendor = Vendor.builder()
                .id(1L)
                .companyName("Test Company")
                .email("vendor@test.com")
                .password("encodedPassword")
                .gstNumber("GST123")
                .registrationId("REG123")
                .phone("1234567890")
                .address("Test Address")
                .contactPerson("John Doe")
                .status(Vendor.VendorStatus.PENDING_APPROVAL)
                .emailVerified(false)
                .compliant(true)
                .registeredAt(LocalDateTime.now())
                .build();

        adminUser = User.builder()
                .id(1L)
                .name("Admin User")
                .email("admin@test.com")
                .role(User.Role.ADMIN)
                .build();

        registerRequest = new VendorRegisterRequest();
        registerRequest.setCompanyName("New Company");
        registerRequest.setEmail("newvendor@test.com");
        registerRequest.setPassword("password123");
        registerRequest.setGstNumber("GST456");
        registerRequest.setRegistrationId("REG456");
        registerRequest.setPhone("9876543210");
        registerRequest.setAddress("New Address");
        registerRequest.setContactPerson("Jane Doe");
    }

    @Test
    void testRegisterVendor_Success() {
        // Arrange
        when(vendorRepo.existsByEmail(anyString())).thenReturn(false);
        when(vendorRepo.existsByGstNumber(anyString())).thenReturn(false);
        when(vendorRepo.existsByRegistrationId(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(vendorRepo.save(any(Vendor.class))).thenReturn(testVendor);
        when(userRepo.findByRole(User.Role.ADMIN)).thenReturn(Arrays.asList(adminUser));

        // Act
        VendorResponse result = vendorService.register(registerRequest);

        // Assert
        assertNotNull(result);
        verify(vendorRepo).save(any(Vendor.class));
        verify(emailService).sendVendorEmailVerification(anyString(), anyString(), anyString(), anyString());
        verify(auditService).log(anyLong(), eq("VENDOR"), anyString(), eq("VENDOR_REGISTERED"), 
                eq("Vendor"), anyLong(), anyString());
    }

    @Test
    void testRegisterVendor_DuplicateEmail() {
        // Arrange
        when(vendorRepo.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateException.class, () -> {
            vendorService.register(registerRequest);
        });
        verify(vendorRepo, never()).save(any(Vendor.class));
    }

    @Test
    void testRegisterVendor_DuplicateGST() {
        // Arrange
        when(vendorRepo.existsByEmail(anyString())).thenReturn(false);
        when(vendorRepo.existsByGstNumber(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateException.class, () -> {
            vendorService.register(registerRequest);
        });
    }

    @Test
    void testApproveVendor_Success() {
        // Arrange
        when(vendorRepo.findById(1L)).thenReturn(Optional.of(testVendor));
        when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(vendorRepo.save(any(Vendor.class))).thenReturn(testVendor);

        // Act
        VendorResponse result = vendorService.approve(1L, "admin@test.com");

        // Assert
        assertNotNull(result);
        verify(vendorRepo).save(any(Vendor.class));
        verify(historyRepo).save(any());
        verify(emailService).sendVendorApprovalEmail(anyString(), anyString(), anyLong());
        verify(auditService).log(anyLong(), eq("USER"), anyString(), eq("VENDOR_APPROVED"), 
                eq("Vendor"), eq(1L), anyString());
    }

    @Test
    void testApproveVendor_InvalidStatus() {
        // Arrange
        testVendor.setStatus(Vendor.VendorStatus.APPROVED);
        when(vendorRepo.findById(1L)).thenReturn(Optional.of(testVendor));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            vendorService.approve(1L, "admin@test.com");
        });
    }

    @Test
    void testRejectVendor_Success() {
        // Arrange
        when(vendorRepo.findById(1L)).thenReturn(Optional.of(testVendor));
        when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(vendorRepo.save(any(Vendor.class))).thenReturn(testVendor);

        // Act
        VendorResponse result = vendorService.reject(1L, "Incomplete documents", "admin@test.com");

        // Assert
        assertNotNull(result);
        verify(vendorRepo).save(any(Vendor.class));
        verify(emailService).sendVendorRejectionEmail(anyString(), anyString(), anyString());
        verify(auditService).log(anyLong(), eq("USER"), anyString(), eq("VENDOR_REJECTED"), 
                eq("Vendor"), eq(1L), anyString());
    }

    @Test
    void testRejectVendor_NoReason() {
        // Arrange
        when(vendorRepo.findById(1L)).thenReturn(Optional.of(testVendor));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            vendorService.reject(1L, "", "admin@test.com");
        });
    }

    @Test
    void testSuspendVendor_Success() {
        // Arrange
        testVendor.setStatus(Vendor.VendorStatus.APPROVED);
        when(vendorRepo.findById(1L)).thenReturn(Optional.of(testVendor));
        when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(vendorRepo.save(any(Vendor.class))).thenReturn(testVendor);

        // Act
        VendorResponse result = vendorService.suspend(1L, "Policy violation", "admin@test.com");

        // Assert
        assertNotNull(result);
        assertEquals(Vendor.VendorStatus.SUSPENDED, testVendor.getStatus());
        verify(vendorRepo).save(any(Vendor.class));
        verify(auditService).log(anyLong(), eq("USER"), anyString(), eq("VENDOR_SUSPENDED"), 
                eq("Vendor"), eq(1L), anyString());
    }

    @Test
    void testGetById_Success() {
        // Arrange
        when(vendorRepo.findById(1L)).thenReturn(Optional.of(testVendor));

        // Act
        VendorResponse result = vendorService.getById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Test Company", result.getCompanyName());
    }

    @Test
    void testGetById_NotFound() {
        // Arrange
        when(vendorRepo.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            vendorService.getById(999L);
        });
    }

    @Test
    void testGetAll_Success() {
        // Arrange
        Page<Vendor> vendorPage = new PageImpl<>(Arrays.asList(testVendor));
        when(vendorRepo.searchVendors(anyString(), any(), any(), any())).thenReturn(vendorPage);

        // Act
        Page<VendorResponse> result = vendorService.getAll("test", Vendor.VendorStatus.PENDING_APPROVAL, 
                true, PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetApproved_Success() {
        // Arrange
        when(vendorRepo.findByStatus(Vendor.VendorStatus.APPROVED)).thenReturn(Arrays.asList(testVendor));

        // Act
        var result = vendorService.getApproved();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetMyProfile_Success() {
        // Arrange
        when(vendorRepo.findByEmail("vendor@test.com")).thenReturn(Optional.of(testVendor));

        // Act
        VendorResponse result = vendorService.getMyProfile("vendor@test.com");

        // Assert
        assertNotNull(result);
        assertEquals("vendor@test.com", result.getEmail());
    }

    @Test
    void testFindByEmail_Success() {
        // Arrange
        when(vendorRepo.findByEmail("vendor@test.com")).thenReturn(Optional.of(testVendor));

        // Act
        Vendor result = vendorService.findByEmail("vendor@test.com");

        // Assert
        assertNotNull(result);
        assertEquals("vendor@test.com", result.getEmail());
    }
}
