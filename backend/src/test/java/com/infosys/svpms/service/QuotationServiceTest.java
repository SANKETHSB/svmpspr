package com.infosys.svpms.service;

import com.infosys.svpms.dto.request.QuotationEvaluationRequest;
import com.infosys.svpms.dto.request.QuotationItemRequest;
import com.infosys.svpms.dto.request.QuotationRequest;
import com.infosys.svpms.dto.response.QuotationResponse;
import com.infosys.svpms.entity.*;
import com.infosys.svpms.exception.BusinessException;
import com.infosys.svpms.exception.DuplicateException;
import com.infosys.svpms.exception.ResourceNotFoundException;
import com.infosys.svpms.repository.*;
import com.infosys.svpms.service.impl.QuotationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuotationServiceTest {

    @Mock
    private QuotationRepository quotationRepo;

    @Mock
    private QuotationDocumentRepository docRepo;

    @Mock
    private RFQRepository rfqRepo;

    @Mock
    private RfqItemRepository rfqItemRepo;

    @Mock
    private VendorRepository vendorRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private RfqVendorInviteRepository inviteRepo;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private QuotationServiceImpl quotationService;

    private Vendor testVendor;
    private RFQ testRfq;
    private Quotation testQuotation;
    private QuotationRequest testQuotationRequest;
    private RfqItem testRfqItem;
    private User testUser;

    @BeforeEach
    void setUp() {
        testVendor = Vendor.builder()
                .id(1L)
                .email("vendor@test.com")
                .companyName("Test Vendor")
                .status(Vendor.VendorStatus.APPROVED)
                .totalRfqsParticipated(0)
                .build();

        testUser = User.builder()
                .id(1L)
                .email("manager@svpms.com")
                .name("Test Manager")
                .role(User.Role.PROCUREMENT_MANAGER)
                .build();

        testRfq = RFQ.builder()
                .id(1L)
                .rfqNumber("RFQ-202605-1001")
                .title("Test RFQ")
                .description("Test Description")
                .deadline(LocalDateTime.now().plusDays(7))
                .status(RFQ.RFQStatus.OPEN)
                .build();

        testRfqItem = RfqItem.builder()
                .id(1L)
                .rfq(testRfq)
                .itemName("Test Item")
                .quantity(10)
                .unit("PCS")
                .build();

        testQuotation = Quotation.builder()
                .id(1L)
                .rfq(testRfq)
                .vendor(testVendor)
                .totalAmount(new BigDecimal("10000.00"))
                .taxPercentage(18.0)
                .currency("INR")
                .deliveryDays(30)
                .status(Quotation.QuotationStatus.SUBMITTED)
                .items(new ArrayList<>())
                .build();

        QuotationItemRequest itemRequest = new QuotationItemRequest();
        itemRequest.setRfqItemId(1L);
        itemRequest.setUnitPrice(new BigDecimal("1000.00"));

        testQuotationRequest = new QuotationRequest();
        testQuotationRequest.setRfqId(1L);
        testQuotationRequest.setTotalAmount(new BigDecimal("10000.00"));
        testQuotationRequest.setTaxPercentage(18.0);
        testQuotationRequest.setCurrency("INR");
        testQuotationRequest.setDeliveryDays(30);
        testQuotationRequest.setNotes("Test Notes");
        testQuotationRequest.setItems(Arrays.asList(itemRequest));
    }

    @Test
    void testSubmitQuotation_Success() {
        // Arrange
        when(vendorRepo.findByEmail(anyString())).thenReturn(Optional.of(testVendor));
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));
        when(inviteRepo.existsByRfqIdAndVendorId(anyLong(), anyLong())).thenReturn(true);
        when(quotationRepo.existsByRfqIdAndVendorId(anyLong(), anyLong())).thenReturn(false);
        when(rfqItemRepo.findById(anyLong())).thenReturn(Optional.of(testRfqItem));
        when(quotationRepo.save(any(Quotation.class))).thenReturn(testQuotation);
        when(inviteRepo.findByRfqIdAndVendorId(anyLong(), anyLong())).thenReturn(Optional.of(new RfqVendorInvite()));
        when(vendorRepo.save(any(Vendor.class))).thenReturn(testVendor);
        when(userRepo.findByRole(any())).thenReturn(new ArrayList<>());
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());
        doNothing().when(notificationService).send(anyLong(), anyString(), anyString(), anyString(), any(), anyLong(), anyString());
        doNothing().when(emailService).sendQuotationSubmissionEmail(anyString(), anyString(), anyString(), anyString(), anyLong(), anyString(), anyString(), anyString());

        // Act
        QuotationResponse response = quotationService.submit(testQuotationRequest, "vendor@test.com");

        // Assert
        assertNotNull(response);
        verify(quotationRepo, times(1)).save(any(Quotation.class));
        verify(vendorRepo, times(1)).save(any(Vendor.class));
    }

    @Test
    void testSubmitQuotation_VendorNotApproved_ThrowsException() {
        // Arrange
        testVendor.setStatus(Vendor.VendorStatus.PENDING_APPROVAL);
        when(vendorRepo.findByEmail(anyString())).thenReturn(Optional.of(testVendor));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            quotationService.submit(testQuotationRequest, "vendor@test.com");
        });
    }

    @Test
    void testSubmitQuotation_RfqAwarded_ThrowsException() {
        // Arrange
        testRfq.setStatus(RFQ.RFQStatus.AWARDED);
        when(vendorRepo.findByEmail(anyString())).thenReturn(Optional.of(testVendor));
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            quotationService.submit(testQuotationRequest, "vendor@test.com");
        });
    }

    @Test
    void testSubmitQuotation_RfqClosed_ThrowsException() {
        // Arrange
        testRfq.setStatus(RFQ.RFQStatus.CLOSED);
        when(vendorRepo.findByEmail(anyString())).thenReturn(Optional.of(testVendor));
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            quotationService.submit(testQuotationRequest, "vendor@test.com");
        });
    }

    @Test
    void testSubmitQuotation_AfterDeadline_ThrowsException() {
        // Arrange
        testRfq.setDeadline(LocalDateTime.now().minusDays(1));
        when(vendorRepo.findByEmail(anyString())).thenReturn(Optional.of(testVendor));
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            quotationService.submit(testQuotationRequest, "vendor@test.com");
        });
    }

    @Test
    void testSubmitQuotation_VendorNotInvited_ThrowsException() {
        // Arrange
        when(vendorRepo.findByEmail(anyString())).thenReturn(Optional.of(testVendor));
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));
        when(inviteRepo.existsByRfqIdAndVendorId(anyLong(), anyLong())).thenReturn(false);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            quotationService.submit(testQuotationRequest, "vendor@test.com");
        });
    }

    @Test
    void testSubmitQuotation_DuplicateSubmission_ThrowsException() {
        // Arrange
        when(vendorRepo.findByEmail(anyString())).thenReturn(Optional.of(testVendor));
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));
        when(inviteRepo.existsByRfqIdAndVendorId(anyLong(), anyLong())).thenReturn(true);
        when(quotationRepo.existsByRfqIdAndVendorId(anyLong(), anyLong())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateException.class, () -> {
            quotationService.submit(testQuotationRequest, "vendor@test.com");
        });
    }

    @Test
    void testResubmitQuotation_Success() {
        // Arrange
        when(quotationRepo.findById(anyLong())).thenReturn(Optional.of(testQuotation));
        when(vendorRepo.findByEmail(anyString())).thenReturn(Optional.of(testVendor));
        when(rfqItemRepo.findById(anyLong())).thenReturn(Optional.of(testRfqItem));
        when(quotationRepo.save(any(Quotation.class))).thenReturn(testQuotation);
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());
        doNothing().when(notificationService).send(anyLong(), anyString(), anyString(), anyString(), any(), anyLong(), anyString());
        doNothing().when(emailService).sendQuotationSubmissionEmail(anyString(), anyString(), anyString(), anyString(), anyLong(), anyString(), anyString(), anyString());

        // Act
        QuotationResponse response = quotationService.resubmit(1L, testQuotationRequest, "vendor@test.com");

        // Assert
        assertNotNull(response);
        verify(quotationRepo, times(1)).save(any(Quotation.class));
    }

    @Test
    void testResubmitQuotation_NotOwner_ThrowsException() {
        // Arrange
        Vendor otherVendor = Vendor.builder().id(2L).email("other@test.com").build();
        when(quotationRepo.findById(anyLong())).thenReturn(Optional.of(testQuotation));
        when(vendorRepo.findByEmail(anyString())).thenReturn(Optional.of(otherVendor));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            quotationService.resubmit(1L, testQuotationRequest, "other@test.com");
        });
    }

    @Test
    void testGetById_Success() {
        // Arrange
        when(quotationRepo.findById(anyLong())).thenReturn(Optional.of(testQuotation));
        when(docRepo.countByQuotationId(anyLong())).thenReturn(0L);

        // Act
        QuotationResponse response = quotationService.getById(1L);

        // Assert
        assertNotNull(response);
        verify(quotationRepo, times(1)).findById(1L);
    }

    @Test
    void testGetById_NotFound_ThrowsException() {
        // Arrange
        when(quotationRepo.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            quotationService.getById(1L);
        });
    }

    @Test
    void testGetByRfq_Success() {
        // Arrange
        when(quotationRepo.findByRfqIdOrderByAmount(anyLong())).thenReturn(Arrays.asList(testQuotation));
        when(docRepo.countByQuotationId(anyLong())).thenReturn(0L);

        // Act
        List<QuotationResponse> responses = quotationService.getByRfq(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void testGetByVendor_Success() {
        // Arrange
        when(quotationRepo.findByVendorId(anyLong())).thenReturn(Arrays.asList(testQuotation));
        when(docRepo.countByQuotationId(anyLong())).thenReturn(0L);

        // Act
        List<QuotationResponse> responses = quotationService.getByVendor(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void testCompareByRfq_Success() {
        // Arrange
        when(quotationRepo.findByRfqIdOrderByAmount(anyLong())).thenReturn(Arrays.asList(testQuotation));
        when(docRepo.countByQuotationId(anyLong())).thenReturn(0L);

        // Act
        List<QuotationResponse> responses = quotationService.compareByRfq(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertTrue(responses.get(0).isLowestBidder());
    }

    @Test
    void testEvaluateQuotation_Success() {
        // Arrange
        QuotationEvaluationRequest evalRequest = new QuotationEvaluationRequest();
        evalRequest.setScore(85.0);
        evalRequest.setComment("Good quotation");

        when(quotationRepo.findById(anyLong())).thenReturn(Optional.of(testQuotation));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(quotationRepo.save(any(Quotation.class))).thenReturn(testQuotation);
        when(docRepo.countByQuotationId(anyLong())).thenReturn(0L);
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());

        // Act
        QuotationResponse response = quotationService.evaluate(1L, evalRequest, "manager@svpms.com");

        // Assert
        assertNotNull(response);
        verify(quotationRepo, times(1)).save(any(Quotation.class));
    }
}
