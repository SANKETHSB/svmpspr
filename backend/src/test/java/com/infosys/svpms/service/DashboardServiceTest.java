package com.infosys.svpms.service;

import com.infosys.svpms.dto.response.DashboardResponse;
import com.infosys.svpms.entity.*;
import com.infosys.svpms.repository.*;
import com.infosys.svpms.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private VendorRepository vendorRepo;

    @Mock
    private RFQRepository rfqRepo;

    @Mock
    private QuotationRepository quotationRepo;

    @Mock
    private PurchaseOrderRepository poRepo;

    @Mock
    private ComplianceDocumentRepository complianceRepo;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Vendor testVendor;
    private RFQ testRfq;
    private Quotation testQuotation;
    private PurchaseOrder testPO;
    private ComplianceDocument testDocument;

    @BeforeEach
    void setUp() {
        testVendor = Vendor.builder()
                .id(1L)
                .companyName("Test Vendor")
                .status(Vendor.VendorStatus.APPROVED)
                .totalRfqsWon(5)
                .performanceScore(85.0)
                .build();

        testRfq = RFQ.builder()
                .id(1L)
                .rfqNumber("RFQ-202605-1001")
                .status(RFQ.RFQStatus.OPEN)
                .build();

        testQuotation = Quotation.builder()
                .id(1L)
                .vendor(testVendor)
                .rfq(testRfq)
                .build();

        testPO = PurchaseOrder.builder()
                .id(1L)
                .vendor(testVendor)
                .status(PurchaseOrder.POStatus.GENERATED)
                .build();

        testDocument = ComplianceDocument.builder()
                .id(1L)
                .vendor(testVendor)
                .expiryDate(LocalDate.now().plusDays(15))
                .expired(false)
                .build();
    }

    @Test
    void testGetStats_Success() {
        // Arrange
        when(vendorRepo.count()).thenReturn(50L);
        when(vendorRepo.findByStatus(Vendor.VendorStatus.PENDING_APPROVAL))
                .thenReturn(Arrays.asList(testVendor));
        when(vendorRepo.findByStatus(Vendor.VendorStatus.APPROVED))
                .thenReturn(Arrays.asList(testVendor, testVendor));
        when(vendorRepo.findByStatus(Vendor.VendorStatus.REJECTED))
                .thenReturn(Arrays.asList());
        when(vendorRepo.findByStatus(Vendor.VendorStatus.SUSPENDED))
                .thenReturn(Arrays.asList());

        when(rfqRepo.count()).thenReturn(100L);
        when(rfqRepo.countByStatus(RFQ.RFQStatus.OPEN)).thenReturn(30L);
        when(rfqRepo.countByStatus(RFQ.RFQStatus.CLOSED)).thenReturn(50L);
        when(rfqRepo.countByStatus(RFQ.RFQStatus.AWARDED)).thenReturn(20L);

        when(quotationRepo.count()).thenReturn(200L);
        when(poRepo.count()).thenReturn(20L);

        when(complianceRepo.findExpiringBefore(any(LocalDate.class)))
                .thenReturn(Arrays.asList(testDocument));
        when(complianceRepo.findExpiredDocuments(any(LocalDate.class)))
                .thenReturn(Arrays.asList());

        // Act
        DashboardResponse response = dashboardService.getStats();

        // Assert
        assertNotNull(response);
        assertEquals(50L, response.getTotalVendors());
        assertEquals(1, response.getPendingVendors());
        assertEquals(2, response.getApprovedVendors());
        assertEquals(0, response.getRejectedVendors());
        assertEquals(0, response.getSuspendedVendors());
        assertEquals(100L, response.getTotalRfqs());
        assertEquals(30L, response.getOpenRfqs());
        assertEquals(50L, response.getClosedRfqs());
        assertEquals(20L, response.getAwardedRfqs());
        assertEquals(200L, response.getTotalQuotations());
        assertEquals(20L, response.getTotalPOs());
        assertEquals(1, response.getExpiringDocuments());
        assertEquals(0, response.getExpiredDocuments());
    }

    @Test
    void testGetStats_NoData_ReturnsZeros() {
        // Arrange
        when(vendorRepo.count()).thenReturn(0L);
        when(vendorRepo.findByStatus(any())).thenReturn(Arrays.asList());
        when(rfqRepo.count()).thenReturn(0L);
        when(rfqRepo.countByStatus(any())).thenReturn(0L);
        when(quotationRepo.count()).thenReturn(0L);
        when(poRepo.count()).thenReturn(0L);
        when(complianceRepo.findExpiringBefore(any(LocalDate.class))).thenReturn(Arrays.asList());
        when(complianceRepo.findExpiredDocuments(any(LocalDate.class))).thenReturn(Arrays.asList());

        // Act
        DashboardResponse response = dashboardService.getStats();

        // Assert
        assertNotNull(response);
        assertEquals(0L, response.getTotalVendors());
        assertEquals(0, response.getPendingVendors());
        assertEquals(0L, response.getTotalRfqs());
        assertEquals(0L, response.getTotalQuotations());
        assertEquals(0L, response.getTotalPOs());
    }

    @Test
    void testGetStats_VendorsByStatus_PopulatedCorrectly() {
        // Arrange
        when(vendorRepo.count()).thenReturn(10L);
        when(vendorRepo.findByStatus(Vendor.VendorStatus.PENDING_APPROVAL))
                .thenReturn(Arrays.asList(testVendor));
        when(vendorRepo.findByStatus(Vendor.VendorStatus.APPROVED))
                .thenReturn(Arrays.asList(testVendor, testVendor, testVendor));
        when(vendorRepo.findByStatus(Vendor.VendorStatus.REJECTED))
                .thenReturn(Arrays.asList(testVendor));
        when(vendorRepo.findByStatus(Vendor.VendorStatus.SUSPENDED))
                .thenReturn(Arrays.asList());

        when(rfqRepo.count()).thenReturn(0L);
        when(rfqRepo.countByStatus(any())).thenReturn(0L);
        when(quotationRepo.count()).thenReturn(0L);
        when(poRepo.count()).thenReturn(0L);
        when(complianceRepo.findExpiringBefore(any(LocalDate.class))).thenReturn(Arrays.asList());
        when(complianceRepo.findExpiredDocuments(any(LocalDate.class))).thenReturn(Arrays.asList());

        // Act
        DashboardResponse response = dashboardService.getStats();

        // Assert
        assertNotNull(response);
        assertNotNull(response.getVendorsByStatus());
        assertEquals(4, response.getVendorsByStatus().size());
    }

    @Test
    void testGetStats_RfqTrend_PopulatedCorrectly() {
        // Arrange
        when(vendorRepo.count()).thenReturn(0L);
        when(vendorRepo.findByStatus(any())).thenReturn(Arrays.asList());
        when(rfqRepo.count()).thenReturn(100L);
        when(rfqRepo.countByStatus(RFQ.RFQStatus.OPEN)).thenReturn(40L);
        when(rfqRepo.countByStatus(RFQ.RFQStatus.CLOSED)).thenReturn(30L);
        when(rfqRepo.countByStatus(RFQ.RFQStatus.AWARDED)).thenReturn(20L);
        when(rfqRepo.countByStatus(RFQ.RFQStatus.ARCHIVED)).thenReturn(10L);
        when(quotationRepo.count()).thenReturn(0L);
        when(poRepo.count()).thenReturn(0L);
        when(complianceRepo.findExpiringBefore(any(LocalDate.class))).thenReturn(Arrays.asList());
        when(complianceRepo.findExpiredDocuments(any(LocalDate.class))).thenReturn(Arrays.asList());

        // Act
        DashboardResponse response = dashboardService.getStats();

        // Assert
        assertNotNull(response);
        assertNotNull(response.getRfqTrend());
        assertEquals(4, response.getRfqTrend().size());
    }

    @Test
    void testGetStats_TopVendors_SortedByWins() {
        // Arrange
        Vendor vendor1 = Vendor.builder()
                .id(1L)
                .companyName("Vendor 1")
                .status(Vendor.VendorStatus.APPROVED)
                .totalRfqsWon(10)
                .performanceScore(90.0)
                .build();

        Vendor vendor2 = Vendor.builder()
                .id(2L)
                .companyName("Vendor 2")
                .status(Vendor.VendorStatus.APPROVED)
                .totalRfqsWon(5)
                .performanceScore(85.0)
                .build();

        when(vendorRepo.count()).thenReturn(2L);
        when(vendorRepo.findByStatus(Vendor.VendorStatus.APPROVED))
                .thenReturn(Arrays.asList(vendor1, vendor2));
        when(vendorRepo.findByStatus(Vendor.VendorStatus.PENDING_APPROVAL))
                .thenReturn(Arrays.asList());
        when(vendorRepo.findByStatus(Vendor.VendorStatus.REJECTED))
                .thenReturn(Arrays.asList());
        when(vendorRepo.findByStatus(Vendor.VendorStatus.SUSPENDED))
                .thenReturn(Arrays.asList());

        when(rfqRepo.count()).thenReturn(0L);
        when(rfqRepo.countByStatus(any())).thenReturn(0L);
        when(quotationRepo.count()).thenReturn(0L);
        when(poRepo.count()).thenReturn(0L);
        when(complianceRepo.findExpiringBefore(any(LocalDate.class))).thenReturn(Arrays.asList());
        when(complianceRepo.findExpiredDocuments(any(LocalDate.class))).thenReturn(Arrays.asList());

        // Act
        DashboardResponse response = dashboardService.getStats();

        // Assert
        assertNotNull(response);
        assertNotNull(response.getTopVendors());
        assertEquals(2, response.getTopVendors().size());
    }

    @Test
    void testGetStats_ComplianceMetrics_Calculated() {
        // Arrange
        ComplianceDocument expiringDoc = ComplianceDocument.builder()
                .id(1L)
                .vendor(testVendor)
                .expiryDate(LocalDate.now().plusDays(15))
                .expired(false)
                .build();

        ComplianceDocument expiredDoc = ComplianceDocument.builder()
                .id(2L)
                .vendor(testVendor)
                .expiryDate(LocalDate.now().minusDays(5))
                .expired(true)
                .build();

        when(vendorRepo.count()).thenReturn(0L);
        when(vendorRepo.findByStatus(any())).thenReturn(Arrays.asList());
        when(rfqRepo.count()).thenReturn(0L);
        when(rfqRepo.countByStatus(any())).thenReturn(0L);
        when(quotationRepo.count()).thenReturn(0L);
        when(poRepo.count()).thenReturn(0L);
        when(complianceRepo.findExpiringBefore(any(LocalDate.class)))
                .thenReturn(Arrays.asList(expiringDoc));
        when(complianceRepo.findExpiredDocuments(any(LocalDate.class)))
                .thenReturn(Arrays.asList(expiredDoc));

        // Act
        DashboardResponse response = dashboardService.getStats();

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getExpiringDocuments());
        assertEquals(1, response.getExpiredDocuments());
    }
}
