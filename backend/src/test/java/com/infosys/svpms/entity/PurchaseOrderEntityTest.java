package com.infosys.svpms.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class PurchaseOrderEntityTest {

    @Test
    void testPurchaseOrderBuilder() {
        LocalDate deliveryDate = LocalDate.now().plusDays(30);
        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber("PO-001")
                .totalAmount(BigDecimal.valueOf(10000))
                .currency("INR")
                .deliveryDate(deliveryDate)
                .shippingAddress("Test Address")
                .status(PurchaseOrder.POStatus.GENERATED)
                .build();

        assertEquals("PO-001", po.getPoNumber());
        assertEquals(BigDecimal.valueOf(10000), po.getTotalAmount());
        assertEquals(PurchaseOrder.POStatus.GENERATED, po.getStatus());
    }

    @Test
    void testPurchaseOrderGettersAndSetters() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(1L);
        po.setPoNumber("PO-002");
        po.setTotalAmount(BigDecimal.valueOf(5000));
        po.setStatus(PurchaseOrder.POStatus.SENT);

        assertEquals(1L, po.getId());
        assertEquals("PO-002", po.getPoNumber());
        assertEquals(PurchaseOrder.POStatus.SENT, po.getStatus());
    }

    @Test
    void testPurchaseOrderStatusEnum() {
        assertEquals(4, PurchaseOrder.POStatus.values().length);
        assertEquals(PurchaseOrder.POStatus.GENERATED, PurchaseOrder.POStatus.valueOf("GENERATED"));
        assertEquals(PurchaseOrder.POStatus.SENT, PurchaseOrder.POStatus.valueOf("SENT"));
        assertEquals(PurchaseOrder.POStatus.RECEIVED, PurchaseOrder.POStatus.valueOf("RECEIVED"));
        assertEquals(PurchaseOrder.POStatus.CLOSED, PurchaseOrder.POStatus.valueOf("CLOSED"));
    }
}
