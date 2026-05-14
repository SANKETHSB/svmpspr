package com.infosys.svpms.service;

import com.infosys.svpms.dto.request.RfqAwardRequest;
import com.infosys.svpms.dto.request.RfqRequest;
import com.infosys.svpms.dto.response.RfqResponse;
import com.infosys.svpms.dto.response.RfqRevisionHistoryResponse;
import com.infosys.svpms.entity.RFQ;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface RfqService {
    RfqResponse create(RfqRequest req, String actorEmail);
    RfqResponse update(Long id, RfqRequest req, String actorEmail);
    RfqResponse getById(Long id);
    /**
     * US 12 AC #2: Filter RFQs by status (Open, Closed, Awarded, Archived)
     * US 12 AC #4: Multi-criteria filtering
     * US 12 AC #5: Pagination support
     * US 12 AC #6: Sorting support
     * 
     * @param search Search term for title or RFQ number
     * @param status RFQ status filter
     * @param createdById Filter by creator user ID
     * @param deadlineFrom Filter RFQs with deadline after this date
     * @param deadlineTo Filter RFQs with deadline before this date
     * @param pageable Pagination and sorting parameters
     * @return Page of RFQs matching criteria
     */
    Page<RfqResponse> getAll(String search, RFQ.RFQStatus status, Long createdById, 
                             java.time.LocalDateTime deadlineFrom, java.time.LocalDateTime deadlineTo, 
                             Pageable pageable);
    Page<RfqResponse> getForVendor(Long vendorId, Pageable pageable);
    RfqResponse inviteVendors(Long rfqId, List<Long> vendorIds, String actorEmail);
    RfqResponse close(Long id, String actorEmail);
    
    /**
     * US 08: Award RFQ to a vendor with comprehensive validation
     * @param rfqId The RFQ to award
     * @param request Award request with quotation ID, reason, and confirmation
     * @param actorEmail The manager awarding the RFQ
     * @return Updated RFQ response
     */
    RfqResponse award(Long rfqId, RfqAwardRequest request, String actorEmail);
    
    /**
     * US 11 AC #6: Admin override to reopen a closed RFQ
     * @param rfqId The RFQ to reopen
     * @param newDeadline New deadline for the reopened RFQ
     * @param reason Reason for reopening (mandatory for audit)
     * @param actorEmail The admin reopening the RFQ
     * @return Updated RFQ response
     */
    RfqResponse reopenRfq(Long rfqId, java.time.LocalDateTime newDeadline, String reason, String actorEmail);
    
    void autoCloseExpired();
    void uploadAttachment(Long rfqId, MultipartFile file, String actorEmail);
    List<Map<String, Object>> getAttachments(Long rfqId);
    List<RfqRevisionHistoryResponse> getRevisionHistory(Long rfqId);
    RfqRevisionHistoryResponse getRevisionByNumber(Long rfqId, int revisionNumber);
    byte[] exportRfqRevision(Long rfqId, int revisionNumber);
}
