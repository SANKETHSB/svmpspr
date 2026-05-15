package com.infosys.svpms.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class RFQEntityTest {

    @Test
    void testRFQBuilder() {
        LocalDateTime deadline = LocalDateTime.now().plusDays(7);
        RFQ rfq = RFQ.builder()
                .rfqNumber("RFQ-001")
                .title("Test RFQ")
                .description("Description")
                .terms("Terms")
                .deadline(deadline)
                .status(RFQ.RFQStatus.OPEN)
                .revisionNumber(1)
                .build();

        assertEquals("RFQ-001", rfq.getRfqNumber());
        assertEquals("Test RFQ", rfq.getTitle());
        assertEquals(RFQ.RFQStatus.OPEN, rfq.getStatus());
        assertEquals(1, rfq.getRevisionNumber());
    }

    @Test
    void testRFQGettersAndSetters() {
        RFQ rfq = new RFQ();
        rfq.setId(1L);
        rfq.setRfqNumber("RFQ-002");
        rfq.setTitle("Title");
        rfq.setDescription("Desc");
        rfq.setStatus(RFQ.RFQStatus.CLOSED);
        rfq.setRevisionNumber(2);

        assertEquals(1L, rfq.getId());
        assertEquals("RFQ-002", rfq.getRfqNumber());
        assertEquals(RFQ.RFQStatus.CLOSED, rfq.getStatus());
        assertEquals(2, rfq.getRevisionNumber());
    }

    @Test
    void testRFQStatusEnum() {
        assertEquals(4, RFQ.RFQStatus.values().length);
        assertEquals(RFQ.RFQStatus.OPEN, RFQ.RFQStatus.valueOf("OPEN"));
        assertEquals(RFQ.RFQStatus.CLOSED, RFQ.RFQStatus.valueOf("CLOSED"));
        assertEquals(RFQ.RFQStatus.AWARDED, RFQ.RFQStatus.valueOf("AWARDED"));
        assertEquals(RFQ.RFQStatus.ARCHIVED, RFQ.RFQStatus.valueOf("ARCHIVED"));
    }

    @Test
    void testRFQDefaults() {
        RFQ rfq = RFQ.builder().build();
        assertEquals(1, rfq.getRevisionNumber());
    }
}
