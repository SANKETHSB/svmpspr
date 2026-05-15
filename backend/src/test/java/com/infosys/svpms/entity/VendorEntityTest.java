package com.infosys.svpms.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class VendorEntityTest {

    @Test
    void testVendorBuilder() {
        Vendor vendor = Vendor.builder()
                .companyName("Test Company")
                .email("vendor@test.com")
                .password("password")
                .gstNumber("GST123456789012")
                .registrationId("REG123")
                .phone("1234567890")
                .address("Test Address")
                .status(Vendor.VendorStatus.APPROVED)
                .compliant(true)
                .build();

        assertEquals("Test Company", vendor.getCompanyName());
        assertEquals("vendor@test.com", vendor.getEmail());
        assertEquals(Vendor.VendorStatus.APPROVED, vendor.getStatus());
        assertTrue(vendor.isCompliant());
    }

    @Test
    void testVendorGettersAndSetters() {
        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setCompanyName("Company");
        vendor.setEmail("test@test.com");
        vendor.setPassword("pass");
        vendor.setGstNumber("GST123");
        vendor.setRegistrationId("REG123");
        vendor.setPhone("1234567890");
        vendor.setAddress("Address");
        vendor.setStatus(Vendor.VendorStatus.PENDING_APPROVAL);
        vendor.setCompliant(false);

        assertEquals(1L, vendor.getId());
        assertEquals("Company", vendor.getCompanyName());
        assertEquals(Vendor.VendorStatus.PENDING_APPROVAL, vendor.getStatus());
        assertFalse(vendor.isCompliant());
    }

    @Test
    void testVendorStatusEnum() {
        assertEquals(4, Vendor.VendorStatus.values().length);
        assertEquals(Vendor.VendorStatus.PENDING_APPROVAL, Vendor.VendorStatus.valueOf("PENDING_APPROVAL"));
        assertEquals(Vendor.VendorStatus.APPROVED, Vendor.VendorStatus.valueOf("APPROVED"));
        assertEquals(Vendor.VendorStatus.REJECTED, Vendor.VendorStatus.valueOf("REJECTED"));
        assertEquals(Vendor.VendorStatus.SUSPENDED, Vendor.VendorStatus.valueOf("SUSPENDED"));
    }

    @Test
    void testVendorDefaults() {
        Vendor vendor = Vendor.builder().build();
        assertTrue(vendor.isCompliant());
    }
}
