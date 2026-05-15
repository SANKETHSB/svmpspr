package com.infosys.svpms.service;

import com.infosys.svpms.dto.response.VendorAnalyticsResponse;
import com.infosys.svpms.entity.*;
import com.infosys.svpms.exception.ResourceNotFoundException;
import com.infosys.svpms.repository.*;
import com.infosys.svpms.service.impl.VendorAnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorAnalyticsServiceTest {

    @Mock
    private VendorRepository vendorRepo;

    @Mock
    private QuotationRepository quotationRepo;

    @Mock
    private PurchaseOrderRepository poRepo;

    @Mock
    private RfqVendorInviteRepository inviteRepo;

    @InjectMocks
    private VendorAnalyticsServiceImpl analyticsService;

    private Vendor testVendor;
    private RFQ testRfq;
    private Quotation testQuotation;
    private PurchaseOrder testPO;

    @BeforeEach
    void setUp() {
        testVendor = Vendor.builder()
                .id(1L)
                .companyName("Test Vendor")
                .email("vendor@test.com")
                .status(Vendor.VendorStatus.APPROVED)
                .totalRfqsParticipated(10)
                .totalRfqsWon(5)
                .performanceScore(85.0)
                .build();

        testRfq = RFQ.builder()
                .id(1L)
                .rfqNumber("RFQ-202605-1001")
                .title("Test RFQ")
                .build();

        testQuotation = Quotation.builder()
                .id(1L)
                .rfq(testRfq)
                .vendor(testVendor)
                .totalAmount(new BigDecimal("10000.00"))
                .currency("INR")
                .awarded(true)
                .weightedScore(85.0)
                .submittedAt(LocalDateTime.now().minusDays(10))
                .build();

        testPO = PurchaseOrder.builder()
                .id(1L)
                .vendor(testVendor)
                .status(PurchaseOrder.POStatus.RECEIVED)
                .generatedAt(LocalDateTime.now().minusDays(5))
                .build();
    }

    @Test
    void testGetVendorAnalytics_Success() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        when(vendorRepo.findById(anyLong())).thenReturn(Optional.of(testVendor));
        when(quotationRepo.findByVendorIdAndSubmittedAtBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testQuotation));
        when(poRepo.findByVendorIdAndGeneratedAtBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testPO));
        when(quotationRepo.findByVendorId(anyLong())).thenReturn(Arrays.asList(testQuotation));
        when(poRepo.findByVendorId(anyLong())).thenReturn(Arrays.asList(testPO));

        // Act
        VendorAnalyticsResponse response = analyticsService.getVendorAnalytics(1L, startDate, endDate);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getVendorId());
        assertEquals("Test Vendor", response.getVendorName());
        assertTrue(response.getPerformanceScore() > 0);
    }

    @Test
    void testGetVendorAnalytics_VendorNotFound_ThrowsException() {
        // Arrange
        when(vendorRepo.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            analyticsService.getVendorAnalytics(1L, LocalDate.now().minusMonths(1), LocalDate.now());
        });
    }

    @Test
    void testGetVendorAnalytics_NoQuotations_ReturnsZeroMetrics() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        when(vendorRepo.findById(anyLong())).thenReturn(Optional.of(testVendor));
        when(quotationRepo.findByVendorIdAndSubmittedAtBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        when(poRepo.findByVendorIdAndGeneratedAtBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        when(quotationRepo.findByVendorId(anyLong())).thenReturn(new ArrayList<>());
        when(poRepo.findByVendorId(anyLong())).thenReturn(new ArrayList<>());

        // Act
        VendorAnalyticsResponse response = analyticsService.getVendorAnalytics(1L, startDate, endDate);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getTotalRfqsParticipated());
        assertEquals(0, response.getTotalRfqsWon());
        assertEquals(0.0, response.getWinRatio());
    }

    @Test
    void testGetAllVendorsAnalytics_Success() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        when(vendorRepo.findByStatus(any())).thenReturn(Arrays.asList(testVendor));
        when(vendorRepo.findById(anyLong())).thenReturn(Optional.of(testVendor));
        when(quotationRepo.findByVendorIdAndSubmittedAtBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testQuotation));
        when(poRepo.findByVendorIdAndGeneratedAtBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testPO));
        when(quotationRepo.findByVendorId(anyLong())).thenReturn(Arrays.asList(testQuotation));
        when(poRepo.findByVendorId(anyLong())).thenReturn(Arrays.asList(testPO));

        // Act
        List<VendorAnalyticsResponse> responses = analyticsService.getAllVendorsAnalytics(startDate, endDate);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void testExportVendorAnalyticsCsv_Success() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        when(vendorRepo.findById(anyLong())).thenReturn(Optional.of(testVendor));
        when(quotationRepo.findByVendorIdAndSubmittedAtBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testQuotation));
        when(poRepo.findByVendorIdAndGeneratedAtBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testPO));
        when(quotationRepo.findByVendorId(anyLong())).thenReturn(Arrays.asList(testQuotation));
        when(poRepo.findByVendorId(anyLong())).thenReturn(Arrays.asList(testPO));

        // Act
        byte[] csvBytes = analyticsService.exportVendorAnalyticsCsv(1L, startDate, endDate);

        // Assert
        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 0);
    }

    @Test
    void testUpdateVendorMetrics_Success() {
        // Arrange
        when(vendorRepo.findById(anyLong())).thenReturn(Optional.of(testVendor));
        when(quotationRepo.findByVendorId(anyLong())).thenReturn(Arrays.asList(testQuotation));
        when(poRepo.findByVendorId(anyLong())).thenReturn(Arrays.asList(testPO));
        when(vendorRepo.save(any(Vendor.class))).thenReturn(testVendor);

        // Act
        analyticsService.updateVendorMetrics(1L);

        // Assert
        verify(vendorRepo, times(1)).save(any(Vendor.class));
    }

    @Test
    void testCalculatePerformanceScore_Success() {
        // Arrange
        when(quotationRepo.findByVendorId(anyLong())).thenReturn(Arrays.asList(testQuotation));
        when(poRepo.findByVendorId(anyLong())).thenReturn(Arrays.asList(testPO));

        // Act
        double score = analyticsService.calculatePerformanceScore(1L);

        // Assert
        assertTrue(score >= 0 && score <= 100);
    }

    @Test
    void testCalculatePerformanceScore_NoData_ReturnsZero() {
        // Arrange
        when(quotationRepo.findByVendorId(anyLong())).thenReturn(new ArrayList<>());
        when(poRepo.findByVendorId(anyLong())).thenReturn(new ArrayList<>());

        // Act
        double score = analyticsService.calculatePerformanceScore(1L);

        // Assert
        assertEquals(0.0, score);
    }
}
