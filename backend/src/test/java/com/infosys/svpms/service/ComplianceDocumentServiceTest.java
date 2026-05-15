package com.infosys.svpms.service;

import com.infosys.svpms.dto.response.ComplianceDocResponse;
import com.infosys.svpms.entity.ComplianceDocument;
import com.infosys.svpms.entity.Vendor;
import com.infosys.svpms.exception.BusinessException;
import com.infosys.svpms.exception.ResourceNotFoundException;
import com.infosys.svpms.repository.ComplianceDocumentRepository;
import com.infosys.svpms.repository.UserRepository;
import com.infosys.svpms.repository.VendorRepository;
import com.infosys.svpms.service.impl.ComplianceDocumentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceDocumentServiceTest {

    @Mock
    private ComplianceDocumentRepository complianceRepo;

    @Mock
    private VendorRepository vendorRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ComplianceDocumentServiceImpl complianceService;

    private Vendor testVendor;
    private ComplianceDocument testDocument;
    private MultipartFile testFile;

    @BeforeEach
    void setUp() {
        testVendor = Vendor.builder()
                .id(1L)
                .email("vendor@test.com")
                .companyName("Test Vendor")
                .status(Vendor.VendorStatus.APPROVED)
                .compliant(true)
                .build();

        testDocument = ComplianceDocument.builder()
                .id(1L)
                .vendor(testVendor)
                .documentType("GST_CERTIFICATE")
                .fileName("gst_cert.pdf")
                .filePath("/uploads/compliance/1/gst_cert.pdf")
                .fileSize(1024L)
                .fileType("application/pdf")
                .issueDate(LocalDate.now().minusMonths(6))
                .expiryDate(LocalDate.now().plusMonths(6))
                .expired(false)
                .version(1)
                .latest(true)
                .build();

        testFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        // Set upload directory for testing
        ReflectionTestUtils.setField(complianceService, "uploadDir", "./test-uploads");
    }

    @Test
    void testUpload_Success() {
        // Arrange
        when(vendorRepo.findById(anyLong())).thenReturn(Optional.of(testVendor));
        when(complianceRepo.findByVendorIdAndDocumentTypeAndLatestTrue(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(complianceRepo.save(any(ComplianceDocument.class))).thenReturn(testDocument);
        when(vendorRepo.findByEmail(anyString())).thenReturn(Optional.of(testVendor));
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), any(), anyString(), anyString());

        // Act
        ComplianceDocResponse response = complianceService.upload(
                1L, "GST_CERTIFICATE", 
                LocalDate.now().minusMonths(6).toString(), 
                LocalDate.now().plusMonths(6).toString(),
                testFile, "vendor@test.com"
        );

        // Assert
        assertNotNull(response);
        verify(complianceRepo, times(1)).save(any(ComplianceDocument.class));
    }

    @Test
    void testUpload_InvalidFileType_ThrowsException() {
        // Arrange
        MultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );
        when(vendorRepo.findById(anyLong())).thenReturn(Optional.of(testVendor));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            complianceService.upload(
                    1L, "GST_CERTIFICATE",
                    LocalDate.now().minusMonths(6).toString(),
                    LocalDate.now().plusMonths(6).toString(),
                    invalidFile, "vendor@test.com"
            );
        });
    }

    @Test
    void testUpload_FileTooLarge_ThrowsException() {
        // Arrange
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        MultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                largeContent
        );
        when(vendorRepo.findById(anyLong())).thenReturn(Optional.of(testVendor));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            complianceService.upload(
                    1L, "GST_CERTIFICATE",
                    LocalDate.now().minusMonths(6).toString(),
                    LocalDate.now().plusMonths(6).toString(),
                    largeFile, "vendor@test.com"
            );
        });
    }

    @Test
    void testUpload_ExpiryBeforeIssue_ThrowsException() {
        // Arrange
        when(vendorRepo.findById(anyLong())).thenReturn(Optional.of(testVendor));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            complianceService.upload(
                    1L, "GST_CERTIFICATE",
                    LocalDate.now().toString(),
                    LocalDate.now().minusDays(1).toString(),
                    testFile, "vendor@test.com"
            );
        });
    }

    @Test
    void testUpload_ExpiryInPast_ThrowsException() {
        // Arrange
        when(vendorRepo.findById(anyLong())).thenReturn(Optional.of(testVendor));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            complianceService.upload(
                    1L, "GST_CERTIFICATE",
                    LocalDate.now().minusMonths(12).toString(),
                    LocalDate.now().minusDays(1).toString(),
                    testFile, "vendor@test.com"
            );
        });
    }

    @Test
    void testUpload_ReUpload_CreatesNewVersion() {
        // Arrange
        ComplianceDocument existingDoc = ComplianceDocument.builder()
                .id(1L)
                .vendor(testVendor)
                .documentType("GST_CERTIFICATE")
                .version(1)
                .latest(true)
                .build();

        when(vendorRepo.findById(anyLong())).thenReturn(Optional.of(testVendor));
        when(complianceRepo.findByVendorIdAndDocumentTypeAndLatestTrue(anyLong(), anyString()))
                .thenReturn(Optional.of(existingDoc));
        doNothing().when(complianceRepo).markPreviousVersionsNotLatest(anyLong(), anyString());
        when(complianceRepo.save(any(ComplianceDocument.class))).thenReturn(testDocument);
        when(vendorRepo.findByEmail(anyString())).thenReturn(Optional.of(testVendor));
        when(vendorRepo.save(any(Vendor.class))).thenReturn(testVendor);
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), any(), anyString(), anyString());

        // Act
        ComplianceDocResponse response = complianceService.upload(
                1L, "GST_CERTIFICATE",
                LocalDate.now().minusMonths(6).toString(),
                LocalDate.now().plusMonths(6).toString(),
                testFile, "vendor@test.com"
        );

        // Assert
        assertNotNull(response);
        verify(complianceRepo, times(1)).markPreviousVersionsNotLatest(1L, "GST_CERTIFICATE");
        verify(vendorRepo, times(1)).save(any(Vendor.class));
    }

    @Test
    void testGetByVendor_Success() {
        // Arrange
        when(complianceRepo.findByVendorIdAndLatestTrue(anyLong()))
                .thenReturn(Arrays.asList(testDocument));

        // Act
        List<ComplianceDocResponse> responses = complianceService.getByVendor(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void testGetVersionHistory_Success() {
        // Arrange
        when(complianceRepo.findByVendorIdAndDocumentTypeOrderByVersionDesc(anyLong(), anyString()))
                .thenReturn(Arrays.asList(testDocument));

        // Act
        List<ComplianceDocResponse> responses = complianceService.getVersionHistory(1L, "GST_CERTIFICATE");

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void testGetComplianceSummary_Success() {
        // Arrange
        when(complianceRepo.count()).thenReturn(100L);
        when(complianceRepo.findExpiredDocuments(any(LocalDate.class))).thenReturn(Arrays.asList(testDocument));
        when(complianceRepo.countExpiringBefore(any(LocalDate.class))).thenReturn(10L);
        when(complianceRepo.countVendorsWithExpiredDocs()).thenReturn(5L);
        when(vendorRepo.count()).thenReturn(50L);
        when(vendorRepo.countByCompliantTrue()).thenReturn(45L);

        // Act
        Map<String, Object> summary = complianceService.getComplianceSummary();

        // Assert
        assertNotNull(summary);
        assertEquals(100L, summary.get("totalDocuments"));
        assertEquals(1L, summary.get("expiredDocuments"));
        assertEquals(10L, summary.get("expiringIn30Days"));
        assertEquals(5L, summary.get("nonCompliantVendors"));
        assertEquals(45L, summary.get("compliantVendors"));
        assertEquals(50L, summary.get("totalVendors"));
    }

    @Test
    void testGetAllVendorComplianceStatus_Success() {
        // Arrange
        when(complianceRepo.findAllLatestOrderByExpiry()).thenReturn(Arrays.asList(testDocument));

        // Act
        List<Map<String, Object>> statuses = complianceService.getAllVendorComplianceStatus();

        // Assert
        assertNotNull(statuses);
        assertEquals(1, statuses.size());
    }

    @Test
    void testDelete_Success() {
        // Arrange
        when(complianceRepo.findById(anyLong())).thenReturn(Optional.of(testDocument));
        when(vendorRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());
        doNothing().when(complianceRepo).delete(any(ComplianceDocument.class));

        // Act
        complianceService.delete(1L, "admin@svpms.com");

        // Assert
        verify(complianceRepo, times(1)).delete(any(ComplianceDocument.class));
    }

    @Test
    void testDelete_NotFound_ThrowsException() {
        // Arrange
        when(complianceRepo.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            complianceService.delete(1L, "admin@svpms.com");
        });
    }
}
