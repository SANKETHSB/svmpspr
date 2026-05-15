package com.infosys.svpms.scheduler;

import com.infosys.svpms.entity.*;
import com.infosys.svpms.repository.*;
import com.infosys.svpms.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SvpmsSchedulerTest {

    @Mock
    private RfqService rfqService;

    @Mock
    private ComplianceDocumentRepository complianceRepo;

    @Mock
    private VendorRepository vendorRepo;

    @Mock
    private NotificationService notifService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private SvpmsScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "warningDays", 30);
    }

    @Test
    void testAutoCloseExpiredRFQs_Success() {
        doNothing().when(rfqService).autoCloseExpired();

        scheduler.autoCloseExpiredRFQs();

        verify(rfqService, times(1)).autoCloseExpired();
    }

    @Test
    void testAutoCloseExpiredRFQs_ServiceThrowsException() {
        doThrow(new RuntimeException("Database error")).when(rfqService).autoCloseExpired();
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), 
                anyString(), anyLong(), anyString());

        scheduler.autoCloseExpiredRFQs();

        verify(rfqService, times(1)).autoCloseExpired();
        verify(auditService, times(1)).log(eq(0L), eq("SYSTEM"), eq("SCHEDULER"), 
                eq("RFQ_AUTO_CLOSE_SCHEDULER_FAILED"), eq("SCHEDULER"), eq(0L), anyString());
    }

    @Test
    void testAutoCloseExpiredRFQs_AuditLogFails() {
        doThrow(new RuntimeException("Service error")).when(rfqService).autoCloseExpired();
        doThrow(new RuntimeException("Audit error")).when(auditService).log(anyLong(), anyString(), 
                anyString(), anyString(), anyString(), anyLong(), anyString());

        scheduler.autoCloseExpiredRFQs();

        verify(rfqService, times(1)).autoCloseExpired();
        verify(auditService, times(1)).log(anyLong(), anyString(), anyString(), anyString(), 
                anyString(), anyLong(), anyString());
    }

    @Test
    void testCheckComplianceExpiry_NoExpiredDocuments() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);

        when(complianceRepo.findExpiredDocuments(today)).thenReturn(Collections.emptyList());
        when(complianceRepo.findExpiringBefore(threshold)).thenReturn(Collections.emptyList());

        scheduler.checkComplianceExpiry();

        verify(complianceRepo, times(1)).findExpiredDocuments(today);
        verify(complianceRepo, times(1)).findExpiringBefore(threshold);
        verify(complianceRepo, never()).save(any(ComplianceDocument.class));
        verify(vendorRepo, never()).save(any(Vendor.class));
        verify(notifService, never()).send(anyLong(), anyString(), anyString(), anyString(), 
                any(Notification.NotificationType.class), anyLong(), anyString());
    }

    @Test
    void testCheckComplianceExpiry_WithExpiredDocuments() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);

        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setCompanyName("Test Vendor");
        vendor.setCompliant(true);

        ComplianceDocument expiredDoc = new ComplianceDocument();
        expiredDoc.setId(1L);
        expiredDoc.setVendor(vendor);
        expiredDoc.setDocumentType("GST_CERTIFICATE");
        expiredDoc.setExpiryDate(today.minusDays(1));
        expiredDoc.setExpired(false);

        when(complianceRepo.findExpiredDocuments(today)).thenReturn(Arrays.asList(expiredDoc));
        when(complianceRepo.findExpiringBefore(threshold)).thenReturn(Collections.emptyList());
        when(complianceRepo.save(any(ComplianceDocument.class))).thenReturn(expiredDoc);
        when(vendorRepo.save(any(Vendor.class))).thenReturn(vendor);
        doNothing().when(notifService).send(anyLong(), anyString(), anyString(), anyString(), 
                any(Notification.NotificationType.class), anyLong(), anyString());
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), 
                anyString(), anyLong(), anyString(), anyString(), anyString());

        scheduler.checkComplianceExpiry();

        verify(complianceRepo, times(1)).save(expiredDoc);
        verify(vendorRepo, times(1)).save(vendor);
        verify(notifService, times(1)).send(eq(1L), eq("VENDOR"), anyString(), anyString(), 
                eq(Notification.NotificationType.COMPLIANCE_EXPIRY), eq(1L), eq("ComplianceDocument"));
        verify(auditService, times(1)).log(eq(1L), eq("VENDOR"), eq("Test Vendor"), 
                eq("COMPLIANCE_EXPIRED"), eq("ComplianceDocument"), eq(1L), 
                eq("isExpired=false"), eq("isExpired=true"), anyString());
    }

    @Test
    void testCheckComplianceExpiry_WithExpiringDocuments() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);

        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setCompanyName("Test Vendor");

        ComplianceDocument expiringDoc = new ComplianceDocument();
        expiringDoc.setId(2L);
        expiringDoc.setVendor(vendor);
        expiringDoc.setDocumentType("PAN_CARD");
        expiringDoc.setExpiryDate(today.plusDays(15));
        expiringDoc.setExpiryWarningSent(false);

        when(complianceRepo.findExpiredDocuments(today)).thenReturn(Collections.emptyList());
        when(complianceRepo.findExpiringBefore(threshold)).thenReturn(Arrays.asList(expiringDoc));
        when(complianceRepo.save(any(ComplianceDocument.class))).thenReturn(expiringDoc);
        doNothing().when(notifService).send(anyLong(), anyString(), anyString(), anyString(), 
                any(Notification.NotificationType.class), anyLong(), anyString());
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), 
                anyString(), anyLong(), anyString());

        scheduler.checkComplianceExpiry();

        verify(complianceRepo, times(1)).save(expiringDoc);
        verify(notifService, times(1)).send(eq(1L), eq("VENDOR"), anyString(), anyString(), 
                eq(Notification.NotificationType.COMPLIANCE_EXPIRY), eq(2L), eq("ComplianceDocument"));
        verify(auditService, times(1)).log(eq(1L), eq("VENDOR"), eq("Test Vendor"), 
                eq("COMPLIANCE_EXPIRY_WARNING"), eq("ComplianceDocument"), eq(2L), anyString());
    }

    @Test
    void testCheckComplianceExpiry_SkipsAlreadyWarnedDocuments() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);

        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setCompanyName("Test Vendor");

        ComplianceDocument alreadyWarnedDoc = new ComplianceDocument();
        alreadyWarnedDoc.setId(3L);
        alreadyWarnedDoc.setVendor(vendor);
        alreadyWarnedDoc.setDocumentType("LICENSE");
        alreadyWarnedDoc.setExpiryDate(today.plusDays(20));
        alreadyWarnedDoc.setExpiryWarningSent(true);

        when(complianceRepo.findExpiredDocuments(today)).thenReturn(Collections.emptyList());
        when(complianceRepo.findExpiringBefore(threshold)).thenReturn(Arrays.asList(alreadyWarnedDoc));

        scheduler.checkComplianceExpiry();

        verify(complianceRepo, never()).save(alreadyWarnedDoc);
        verify(notifService, never()).send(anyLong(), anyString(), anyString(), anyString(), 
                any(Notification.NotificationType.class), anyLong(), anyString());
    }

    @Test
    void testCheckComplianceExpiry_MultipleExpiredAndExpiringDocuments() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);

        Vendor vendor1 = new Vendor();
        vendor1.setId(1L);
        vendor1.setCompanyName("Vendor 1");
        vendor1.setCompliant(true);

        Vendor vendor2 = new Vendor();
        vendor2.setId(2L);
        vendor2.setCompanyName("Vendor 2");

        ComplianceDocument expiredDoc1 = new ComplianceDocument();
        expiredDoc1.setId(1L);
        expiredDoc1.setVendor(vendor1);
        expiredDoc1.setDocumentType("GST");
        expiredDoc1.setExpiryDate(today.minusDays(5));
        expiredDoc1.setExpired(false);

        ComplianceDocument expiredDoc2 = new ComplianceDocument();
        expiredDoc2.setId(2L);
        expiredDoc2.setVendor(vendor1);
        expiredDoc2.setDocumentType("PAN");
        expiredDoc2.setExpiryDate(today.minusDays(1));
        expiredDoc2.setExpired(false);

        ComplianceDocument expiringDoc = new ComplianceDocument();
        expiringDoc.setId(3L);
        expiringDoc.setVendor(vendor2);
        expiringDoc.setDocumentType("LICENSE");
        expiringDoc.setExpiryDate(today.plusDays(10));
        expiringDoc.setExpiryWarningSent(false);

        when(complianceRepo.findExpiredDocuments(today))
                .thenReturn(Arrays.asList(expiredDoc1, expiredDoc2));
        when(complianceRepo.findExpiringBefore(threshold))
                .thenReturn(Arrays.asList(expiringDoc));
        when(complianceRepo.save(any(ComplianceDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(vendorRepo.save(any(Vendor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(notifService).send(anyLong(), anyString(), anyString(), anyString(), 
                any(Notification.NotificationType.class), anyLong(), anyString());
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), 
                anyString(), anyLong(), anyString(), anyString(), anyString());
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), 
                anyString(), anyLong(), anyString());

        scheduler.checkComplianceExpiry();

        verify(complianceRepo, times(3)).save(any(ComplianceDocument.class));
        verify(vendorRepo, times(2)).save(vendor1); // Same vendor, saved twice for 2 expired docs
        verify(notifService, times(3)).send(anyLong(), anyString(), anyString(), anyString(), 
                any(Notification.NotificationType.class), anyLong(), anyString());
        verify(auditService, times(2)).log(anyLong(), anyString(), anyString(), 
                eq("COMPLIANCE_EXPIRED"), anyString(), anyLong(), anyString(), anyString(), anyString());
        verify(auditService, times(1)).log(anyLong(), anyString(), anyString(), 
                eq("COMPLIANCE_EXPIRY_WARNING"), anyString(), anyLong(), anyString());
    }

    @Test
    void testCheckComplianceExpiry_ExceptionHandling() {
        when(complianceRepo.findExpiredDocuments(any(LocalDate.class)))
                .thenThrow(new RuntimeException("Database connection error"));

        scheduler.checkComplianceExpiry();

        verify(complianceRepo, times(1)).findExpiredDocuments(any(LocalDate.class));
        verify(complianceRepo, never()).save(any(ComplianceDocument.class));
    }
}
