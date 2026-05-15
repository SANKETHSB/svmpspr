package com.infosys.svpms.service;

import com.infosys.svpms.dto.request.RfqAwardRequest;
import com.infosys.svpms.dto.request.RfqItemRequest;
import com.infosys.svpms.dto.request.RfqRequest;
import com.infosys.svpms.dto.response.RfqResponse;
import com.infosys.svpms.entity.*;
import com.infosys.svpms.exception.BusinessException;
import com.infosys.svpms.exception.ResourceNotFoundException;
import com.infosys.svpms.repository.*;
import com.infosys.svpms.service.impl.RfqServiceImpl;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RfqServiceTest {

    @Mock
    private RFQRepository rfqRepo;

    @Mock
    private RfqItemRepository itemRepo;

    @Mock
    private RfqVendorInviteRepository inviteRepo;

    @Mock
    private RfqAttachmentRepository attachmentRepo;

    @Mock
    private RfqRevisionHistoryRepository revisionHistoryRepo;

    @Mock
    private VendorRepository vendorRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private QuotationRepository quotationRepo;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RfqServiceImpl rfqService;

    private User testUser;
    private Vendor testVendor;
    private RFQ testRfq;
    private RfqRequest testRfqRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("manager@svpms.com")
                .name("Test Manager")
                .role(User.Role.PROCUREMENT_MANAGER)
                .active(true)
                .build();

        testVendor = Vendor.builder()
                .id(1L)
                .email("vendor@test.com")
                .companyName("Test Vendor")
                .status(Vendor.VendorStatus.APPROVED)
                .compliant(true)
                .build();

        testRfq = RFQ.builder()
                .id(1L)
                .rfqNumber("RFQ-202605-1001")
                .title("Test RFQ")
                .description("Test Description")
                .terms("Test Terms")
                .deadline(LocalDateTime.now().plusDays(7))
                .status(RFQ.RFQStatus.OPEN)
                .createdBy(testUser)
                .revisionNumber(1)
                .items(new ArrayList<>())
                .build();

        RfqItemRequest itemRequest = new RfqItemRequest();
        itemRequest.setItemName("Test Item");
        itemRequest.setDescription("Test Item Description");
        itemRequest.setQuantity(10);
        itemRequest.setUnit("PCS");
        itemRequest.setSpecifications("Test Specs");

        testRfqRequest = new RfqRequest();
        testRfqRequest.setTitle("Test RFQ");
        testRfqRequest.setDescription("Test Description");
        testRfqRequest.setTerms("Test Terms");
        testRfqRequest.setDeadline(LocalDateTime.now().plusDays(7));
        testRfqRequest.setItems(Arrays.asList(itemRequest));
        testRfqRequest.setInvitedVendorIds(Arrays.asList(1L));
    }

    @Test
    void testCreateRfq_Success() {
        // Arrange
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(rfqRepo.findByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(new ArrayList<>());
        when(rfqRepo.save(any(RFQ.class))).thenReturn(testRfq);
        when(vendorRepo.findAllById(anyList())).thenReturn(Arrays.asList(testVendor));
        when(inviteRepo.save(any(RfqVendorInvite.class))).thenReturn(new RfqVendorInvite());
        doNothing().when(notificationService).send(anyLong(), anyString(), anyString(), anyString(), any(), anyLong(), anyString());
        doNothing().when(emailService).sendRfqAssignmentEmail(anyString(), anyString(), anyString(), anyString(), anyString());
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());

        // Act
        RfqResponse response = rfqService.create(testRfqRequest, "manager@svpms.com");

        // Assert
        assertNotNull(response);
        verify(rfqRepo, times(1)).save(any(RFQ.class));
        verify(inviteRepo, times(1)).save(any(RfqVendorInvite.class));
        verify(notificationService, times(1)).send(anyLong(), anyString(), anyString(), anyString(), any(), anyLong(), anyString());
        verify(emailService, times(1)).sendRfqAssignmentEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testCreateRfq_DeadlineInPast_ThrowsException() {
        // Arrange
        testRfqRequest.setDeadline(LocalDateTime.now().minusDays(1));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            rfqService.create(testRfqRequest, "manager@svpms.com");
        });
    }

    @Test
    void testCreateRfq_NoItems_ThrowsException() {
        // Arrange
        testRfqRequest.setItems(new ArrayList<>());
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            rfqService.create(testRfqRequest, "manager@svpms.com");
        });
    }

    @Test
    void testCreateRfq_NonCompliantVendor_ThrowsException() {
        // Arrange
        testVendor.setCompliant(false);
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(rfqRepo.findByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(new ArrayList<>());
        when(rfqRepo.save(any(RFQ.class))).thenReturn(testRfq);
        when(vendorRepo.findAllById(anyList())).thenReturn(Arrays.asList(testVendor));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            rfqService.create(testRfqRequest, "manager@svpms.com");
        });
    }

    @Test
    void testUpdateRfq_Success() {
        // Arrange
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(rfqRepo.save(any(RFQ.class))).thenReturn(testRfq);
        when(inviteRepo.findByRfqId(anyLong())).thenReturn(new ArrayList<>());
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());

        // Act
        RfqResponse response = rfqService.update(1L, testRfqRequest, "manager@svpms.com");

        // Assert
        assertNotNull(response);
        verify(rfqRepo, times(1)).save(any(RFQ.class));
    }

    @Test
    void testUpdateRfq_AfterAward_ThrowsException() {
        // Arrange
        testRfq.setStatus(RFQ.RFQStatus.AWARDED);
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            rfqService.update(1L, testRfqRequest, "manager@svpms.com");
        });
    }

    @Test
    void testUpdateRfq_AfterClosed_ThrowsException() {
        // Arrange
        testRfq.setStatus(RFQ.RFQStatus.CLOSED);
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            rfqService.update(1L, testRfqRequest, "manager@svpms.com");
        });
    }

    @Test
    void testGetById_Success() {
        // Arrange
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));

        // Act
        RfqResponse response = rfqService.getById(1L);

        // Assert
        assertNotNull(response);
        verify(rfqRepo, times(1)).findById(1L);
    }

    @Test
    void testGetById_NotFound_ThrowsException() {
        // Arrange
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            rfqService.getById(1L);
        });
    }

    @Test
    void testGetAll_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<RFQ> rfqPage = new PageImpl<>(Arrays.asList(testRfq));
        when(rfqRepo.searchRFQs(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(rfqPage);

        // Act
        Page<RfqResponse> response = rfqService.getAll("test", null, null, null, null, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void testCloseRfq_Success() {
        // Arrange
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(rfqRepo.save(any(RFQ.class))).thenReturn(testRfq);
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());

        // Act
        RfqResponse response = rfqService.close(1L, "manager@svpms.com");

        // Assert
        assertNotNull(response);
        verify(rfqRepo, times(1)).save(any(RFQ.class));
    }

    @Test
    void testReopenRfq_Success() {
        // Arrange
        testRfq.setStatus(RFQ.RFQStatus.CLOSED);
        LocalDateTime newDeadline = LocalDateTime.now().plusDays(7);
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(rfqRepo.save(any(RFQ.class))).thenReturn(testRfq);
        when(inviteRepo.findByRfqId(anyLong())).thenReturn(new ArrayList<>());
        when(userRepo.findByRole(any())).thenReturn(new ArrayList<>());
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());

        // Act
        RfqResponse response = rfqService.reopenRfq(1L, newDeadline, "Valid reason for reopening", "manager@svpms.com");

        // Assert
        assertNotNull(response);
        verify(rfqRepo, times(1)).save(any(RFQ.class));
    }

    @Test
    void testReopenRfq_NotClosed_ThrowsException() {
        // Arrange
        testRfq.setStatus(RFQ.RFQStatus.OPEN);
        LocalDateTime newDeadline = LocalDateTime.now().plusDays(7);
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            rfqService.reopenRfq(1L, newDeadline, "Valid reason", "manager@svpms.com");
        });
    }

    @Test
    void testInviteVendors_Success() {
        // Arrange
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(inviteRepo.existsByRfqIdAndVendorId(anyLong(), anyLong())).thenReturn(false);
        when(vendorRepo.findById(anyLong())).thenReturn(Optional.of(testVendor));
        when(inviteRepo.save(any(RfqVendorInvite.class))).thenReturn(new RfqVendorInvite());
        when(rfqRepo.findById(anyLong())).thenReturn(Optional.of(testRfq));
        doNothing().when(notificationService).send(anyLong(), anyString(), anyString(), anyString(), any(), anyLong(), anyString());
        doNothing().when(emailService).sendRfqAssignmentEmail(anyString(), anyString(), anyString(), anyString(), anyString());
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());

        // Act
        RfqResponse response = rfqService.inviteVendors(1L, Arrays.asList(1L), "manager@svpms.com");

        // Assert
        assertNotNull(response);
        verify(inviteRepo, times(1)).save(any(RfqVendorInvite.class));
    }
}
