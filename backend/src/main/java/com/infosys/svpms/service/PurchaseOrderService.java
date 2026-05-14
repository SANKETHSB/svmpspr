package com.infosys.svpms.service;

import com.infosys.svpms.dto.request.PORequest;
import com.infosys.svpms.dto.response.POResponse;
import com.infosys.svpms.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseOrderService {
    POResponse generate(Long rfqId, PORequest req, String actorEmail);
    POResponse getById(Long id);
    /**
     * US 12 AC #3: Filter POs by vendor, date range, and status
     * US 12 AC #4: Multi-criteria filtering
     * US 12 AC #5: Pagination support
     * US 12 AC #6: Sorting support
     * 
     * @param search Search term for PO number or RFQ number
     * @param vendorId Vendor ID filter
     * @param status PO status filter
     * @param dateFrom Generated date from filter
     * @param dateTo Generated date to filter
     * @param pageable Pagination and sorting parameters
     * @return Page of POs matching criteria
     */
    Page<POResponse> getAll(String search, Long vendorId, PurchaseOrder.POStatus status, 
                            java.time.LocalDateTime dateFrom, java.time.LocalDateTime dateTo, 
                            Pageable pageable);
    POResponse updateStatus(Long id, PurchaseOrder.POStatus status, String actorEmail);
    byte[] exportPdf(Long id);
}
