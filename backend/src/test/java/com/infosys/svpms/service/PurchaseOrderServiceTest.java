package com.infosys.svpms.service;

import com.infosys.svpms.dto.request.PORequest;
import com.infosys.svpms.dto.response.POResponse;
import com.infosys.svpms.entity.*;
import com.infosys.svpms.exception.BusinessException;
import com.infosys.svpms.exception.DuplicateException;
import com.infosys.svpms.exception.ResourceNotFoundException;
import com.infosys.svpms.repository.*;
import com.infosys.svpms.service.impl.PurchaseOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository poRepo;

    @Mock
    private RFQRepository rfqRepo;

    @Mock
    private QuotationRepository quotationRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PurchaseOrderServiceImpl poService;

    private User testUser;
    private Vendor testVendor;
    private RFQ testRfq;
    private Quotation testQuotation;
    private PurchaseOrder testPO;
    private PORequest testPORequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("manager@svpms.com")
                .name("Test Manager")
                .role(User.Role.PROCUREMENT_MANAGER)
                .build();

        testVendor = Vendor.builder()
                .id(1L)
                .email("vendor@test.com")
                .companyName("Test Vendor")
                .status(Vendor.VendorStatus.APPROVED)
                .build();

        testRfq = RFQ.builder()
                .id(1L)
                .rfqNumber("RFQ-202605-1001")
                .title("Test RFQ")
                .status(RFQ.RFQStatus.AWARDED)
                .awardedVendor(testVendor)
                .build();

        RfqItem rfqItem = RfqItem.builder()
                .id(1L)
                .itemName("Test Item")
                .quantity(10)
                .unit("PCS")
                .build();

        QuotationItem quotationItem = QuotationItem.builder()
                .id(1L)
                .rfqItem(rfqItem)
                .unitPrice(new BigDecimal("1000.00"))
                .totalPrice(new BigDecimal("10000.00"))
                .build();

        testQuotation = Quotation.builder()
                .id(1L)
                .rfq(testRfq)
                .vendor(testVendor)
                .totalAmount(new BigDecimal("10000.00"))
                .taxPercentage(18.0)
                .currency("INR")
                .deliveryDays(30)
                .awarded(true)
                .items(Arrays.asList(quotationItem))
                .build();

        testPO = PurchaseOrder.builder()
                .id(1L)
                .poNumber("PO-202605-5001")
                .rfq(testRfq)
                .quotation(testQuotation)
                .vendor(testVendor)
                .totalAmount(new BigDecimal("11800.00"))
                .currency("INR")
                .status(PurchaseOrder.POStatus.GENERATED)
                .generatedBy(testUser)
                .generatedAt(LocalDateTime.now())
                .build();

        testPORequest = new PORequest();
        testPORequest.setDeliveryDate(LocalDate.now().plusDays(30));
        testPORequest.setShippingAddress("Test Shipping Address, City, State - 123456");
        testPORequest.setPaymentTerms("Net 30 days");
        testPORequest.setSpecialInstructions("Test special instructions");

        // Set upload directory for testing
        ReflectionTestUtils.setField(poService, "uploadDir", "./test-uploads");
    }

    @Test
    void testGeneratePO_Success() {
        // Arrange
        when(poRepo.existsByRfqId(anyLong())).thenReturn(false);
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(quotationRepo.findByRfqId(anyLong())).thenReturn(Arrays.asList(testQuotation));
        when(poRepo.save(any(PurchaseOrder.class))).thenReturn(testPO);
        doNothing().when(notificationService).send(anyLong(), anyString(), anyString(), anyString(), any(), anyLong(), anyString());
        doNothing().when(emailService).sendPoIssuanceEmail(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());

        // Act
        POResponse response = poService.generate(1L, testPORequest, "manager@svpms.com");

        // Assert
        assertNotNull(response);
        verify(poRepo, times(2)).save(any(PurchaseOrder.class)); // Once for initial save, once for PDF path update
        verify(notificationService, times(2)).send(anyLong(), anyString(), anyString(), anyString(), any(), anyLong(), anyString());
        verify(emailService, times(1)).sendPoIssuanceEmail(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testGeneratePO_DuplicatePO_ThrowsException() {
        // Arrange
        when(poRepo.existsByRfqId(anyLong())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateException.class, () -> {
            poService.generate(1L, testPORequest, "manager@svpms.com");
        });
    }

    @Test
    void testGeneratePO_RfqNotAwarded_ThrowsException() {
        // Arrange
        testRfq.setStatus(RFQ.RFQStatus.OPEN);
        when(poRepo.existsByRfqId(anyLong())).thenReturn(false);
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            poService.generate(1L, testPORequest, "manager@svpms.com");
        });
    }

    @Test
    void testGeneratePO_DeliveryDateInPast_ThrowsException() {
        // Arrange
        testPORequest.setDeliveryDate(LocalDate.now().minusDays(1));
        when(poRepo.existsByRfqId(anyLong())).thenReturn(false);
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(quotationRepo.findByRfqId(anyLong())).thenReturn(Arrays.asList(testQuotation));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            poService.generate(1L, testPORequest, "manager@svpms.com");
        });
    }

    @Test
    void testGetById_Success() {
        // Arrange
        when(poRepo.findById(anyLong())).thenReturn(Optional.of(testPO));

        // Act
        POResponse response = poService.getById(1L);

        // Assert
        assertNotNull(response);
        verify(poRepo, times(1)).findById(1L);
    }

    @Test
    void testGetById_NotFound_ThrowsException() {
        // Arrange
        when(poRepo.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            poService.getById(1L);
        });
    }

    @Test
    void testGetAll_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<PurchaseOrder> poPage = new PageImpl<>(Arrays.asList(testPO));
        when(poRepo.filterPOs(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(poPage);

        // Act
        Page<POResponse> response = poService.getAll("test", null, null, null, null, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void testUpdateStatus_Success() {
        // Arrange
        when(poRepo.findById(anyLong())).thenReturn(Optional.of(testPO));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(poRepo.save(any(PurchaseOrder.class))).thenReturn(testPO);
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());
        doNothing().when(notificationService).send(anyLong(), anyString(), anyString(), anyString(), any(), anyLong(), anyString());

        // Act
        POResponse response = poService.updateStatus(1L, PurchaseOrder.POStatus.SENT, "manager@svpms.com");

        // Assert
        assertNotNull(response);
        verify(poRepo, times(1)).save(any(PurchaseOrder.class));
    }

    @Test
    void testExportPdf_Success() {
        // Arrange
        when(poRepo.findById(anyLong())).thenReturn(Optional.of(testPO));

        // Act
        byte[] pdfBytes = poService.exportPdf(1L);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testExportPdf_NotFound_ThrowsException() {
        // Arrange
        when(poRepo.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            poService.exportPdf(1L);
        });
    }
}
