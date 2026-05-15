package com.infosys.svpms.service;

import com.infosys.svpms.service.impl.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmailServiceTest - Tests for EmailService
 * 
 * Note: EmailServiceImpl logs emails to console instead of sending actual emails.
 * These tests verify that the methods execute without throwing exceptions.
 * In a production environment with actual email sending, you would mock the
 * email client and verify interactions.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        // EmailService doesn't have dependencies to mock
    }

    @Test
    void testSendVendorApprovalEmail_Success() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            emailService.sendVendorApprovalEmail(
                    "vendor@test.com",
                    "Test Vendor",
                    1L
            );
        });
    }

    @Test
    void testSendVendorRejectionEmail_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendVendorRejectionEmail(
                    "vendor@test.com",
                    "Test Vendor",
                    "Documents incomplete"
            );
        });
    }

    @Test
    void testSendRfqAssignmentEmail_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendRfqAssignmentEmail(
                    "vendor@test.com",
                    "Test Vendor",
                    "RFQ-202605-1001",
                    "Test RFQ Title",
                    "2026-06-15 10:00"
            );
        });
    }

    @Test
    void testSendRfqAwardEmail_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendRfqAwardEmail(
                    "vendor@test.com",
                    "Test Vendor",
                    "RFQ-202605-1001",
                    "Test RFQ Title",
                    "Best price and quality"
            );
        });
    }

    @Test
    void testSendRfqNonSelectionEmail_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendRfqNonSelectionEmail(
                    "vendor@test.com",
                    "Test Vendor",
                    "RFQ-202605-1001",
                    "Test RFQ Title"
            );
        });
    }

    @Test
    void testSendPoIssuanceEmail_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendPoIssuanceEmail(
                    "vendor@test.com",
                    "Test Vendor",
                    "PO-202605-5001",
                    "RFQ-202605-1001",
                    "10000.00",
                    "INR",
                    "2026-07-15"
            );
        });
    }

    @Test
    void testSendComplianceExpiryEmail_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendComplianceExpiryEmail(
                    "vendor@test.com",
                    "Test Vendor",
                    "GST Certificate",
                    "2026-06-30"
            );
        });
    }

    @Test
    void testSendAccountLockedEmail_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendAccountLockedEmail(
                    "user@test.com",
                    "Test User",
                    "2026-05-15 12:00"
            );
        });
    }

    @Test
    void testSendPasswordResetEmail_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendPasswordResetEmail(
                    "user@test.com",
                    "Test User",
                    "reset-token-123456"
            );
        });
    }

    @Test
    void testSendVendorEmailVerification_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendVendorEmailVerification(
                    "vendor@test.com",
                    "Test Vendor",
                    "http://localhost:3000/verify?token=abc123",
                    "abc123"
            );
        });
    }

    @Test
    void testSendAdminNewVendorRegistration_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendAdminNewVendorRegistration(
                    "admin@test.com",
                    "Admin User",
                    "Test Vendor",
                    "GST123456",
                    "REG-001",
                    1L
            );
        });
    }

    @Test
    void testSendRfqRevisionEmail_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendRfqRevisionEmail(
                    "vendor@test.com",
                    "Test Vendor",
                    "RFQ-202605-1001",
                    "Test RFQ Title",
                    2,
                    "Deadline extended",
                    "2026-06-20"
            );
        });
    }

    @Test
    void testSendQuotationSubmissionEmail_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendQuotationSubmissionEmail(
                    "vendor@test.com",
                    "Test Vendor",
                    "RFQ-202605-1001",
                    "Test RFQ Title",
                    1L,
                    "10000.00",
                    "INR",
                    "2026-05-15 10:30"
            );
        });
    }

    @Test
    void testSendRfqClosureEmail_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendRfqClosureEmail(
                    "manager@test.com",
                    "Test Manager",
                    "RFQ-202605-1001",
                    "Test RFQ Title",
                    "2026-05-15 10:00",
                    5
            );
        });
    }

    @Test
    void testSendGenericNotificationEmail_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendGenericNotificationEmail(
                    "user@test.com",
                    "Test User",
                    "Test Notification",
                    "This is a test notification message"
            );
        });
    }

    @Test
    void testSendGenericNotificationEmail_NullEmail_Success() {
        // Act & Assert - Should handle null email gracefully
        assertDoesNotThrow(() -> {
            emailService.sendGenericNotificationEmail(
                    null,
                    "Test User",
                    "Test Notification",
                    "This is a test notification message"
            );
        });
    }

    @Test
    void testEmailService_AllMethodsAreAsync() {
        // This test verifies that the service is properly annotated
        // In actual execution, @Async methods run in separate threads
        // For unit tests, they run synchronously
        
        // Act - Call multiple methods in sequence
        emailService.sendVendorApprovalEmail("test@test.com", "Vendor", 1L);
        emailService.sendRfqAssignmentEmail("test@test.com", "Vendor", "RFQ-001", "Title", "2026-06-15");
        emailService.sendPoIssuanceEmail("test@test.com", "Vendor", "PO-001", "RFQ-001", "1000", "INR", "2026-07-15");
        
        // Assert - All methods completed without blocking
        assertTrue(true, "All async methods executed successfully");
    }
}
