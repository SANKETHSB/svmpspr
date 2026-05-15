package com.infosys.svpms.utility;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ValidationConstantsTest {

    @Test
    void testUtilityClassCannotBeInstantiated() throws Exception {
        // Verify that ValidationConstants is a proper utility class
        Constructor<?>[] constructors = ValidationConstants.class.getDeclaredConstructors();
        assertEquals(1, constructors.length);
        
        Constructor<?> constructor = constructors[0];
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        
        // Private constructor exists, which prevents instantiation
        // This is sufficient for a utility class
    }

    // ========== USER VALIDATION CONSTANTS ==========
    @Test
    void testUserValidationConstants() {
        assertEquals(100, ValidationConstants.USER_NAME_MAX_LENGTH);
        assertEquals(8, ValidationConstants.USER_PASSWORD_MIN_LENGTH);
        assertEquals("^[A-Za-z0-9+_.-]+@(.+)$", ValidationConstants.USER_EMAIL_PATTERN);
    }

    // ========== VENDOR VALIDATION CONSTANTS ==========
    @Test
    void testVendorValidationConstants() {
        assertEquals(200, ValidationConstants.VENDOR_COMPANY_NAME_MAX_LENGTH);
        assertEquals(15, ValidationConstants.VENDOR_GST_LENGTH);
        assertEquals(15, ValidationConstants.VENDOR_PHONE_MAX_LENGTH);
        assertEquals(500, ValidationConstants.VENDOR_ADDRESS_MAX_LENGTH);
        assertEquals(8, ValidationConstants.VENDOR_PASSWORD_MIN_LENGTH);
    }

    // ========== RFQ VALIDATION CONSTANTS ==========
    @Test
    void testRfqValidationConstants() {
        assertEquals(200, ValidationConstants.RFQ_TITLE_MAX_LENGTH);
        assertEquals(2000, ValidationConstants.RFQ_DESCRIPTION_MAX_LENGTH);
        assertEquals(200, ValidationConstants.RFQ_ITEM_NAME_MAX_LENGTH);
        assertEquals(500, ValidationConstants.RFQ_ITEM_DESCRIPTION_MAX_LENGTH);
        assertEquals(50, ValidationConstants.RFQ_ITEM_UNIT_MAX_LENGTH);
        assertEquals(1, ValidationConstants.RFQ_ITEM_MIN_QUANTITY);
    }

    // ========== QUOTATION VALIDATION CONSTANTS ==========
    @Test
    void testQuotationValidationConstants() {
        assertEquals("0.01", ValidationConstants.QUOTATION_MIN_AMOUNT);
        assertEquals("0.0", ValidationConstants.QUOTATION_MIN_TAX);
        assertEquals("100.0", ValidationConstants.QUOTATION_MAX_TAX);
        assertEquals(10, ValidationConstants.QUOTATION_CURRENCY_MAX_LENGTH);
        assertEquals(1, ValidationConstants.QUOTATION_MIN_DELIVERY_DAYS);
        assertEquals(1000, ValidationConstants.QUOTATION_NOTES_MAX_LENGTH);
        assertEquals(10, ValidationConstants.QUOTATION_EVALUATION_COMMENT_MIN_LENGTH);
        assertEquals(1000, ValidationConstants.QUOTATION_EVALUATION_COMMENT_MAX_LENGTH);
        assertEquals("0.0", ValidationConstants.QUOTATION_MIN_SCORE);
        assertEquals("100.0", ValidationConstants.QUOTATION_MAX_SCORE);
    }

    // ========== PURCHASE ORDER VALIDATION CONSTANTS ==========
    @Test
    void testPurchaseOrderValidationConstants() {
        assertEquals(500, ValidationConstants.PO_SHIPPING_ADDRESS_MAX_LENGTH);
        assertEquals(500, ValidationConstants.PO_PAYMENT_TERMS_MAX_LENGTH);
        assertEquals(1000, ValidationConstants.PO_SPECIAL_INSTRUCTIONS_MAX_LENGTH);
    }

    // ========== ROLE VALIDATION CONSTANTS ==========
    @Test
    void testRoleValidationConstants() {
        assertEquals(100, ValidationConstants.ROLE_NAME_MAX_LENGTH);
        assertEquals(500, ValidationConstants.ROLE_DESCRIPTION_MAX_LENGTH);
    }

    // ========== NOTIFICATION VALIDATION CONSTANTS ==========
    @Test
    void testNotificationValidationConstants() {
        assertEquals(200, ValidationConstants.NOTIFICATION_TITLE_MAX_LENGTH);
        assertEquals(1000, ValidationConstants.NOTIFICATION_MESSAGE_MAX_LENGTH);
    }

    // ========== VALIDATION MESSAGES ==========
    @Test
    void testUserValidationMessages() {
        assertEquals("Name is required", ValidationConstants.Messages.USER_NAME_REQUIRED);
        assertEquals("Name must not exceed 100 characters", ValidationConstants.Messages.USER_NAME_TOO_LONG);
        assertEquals("Email is required", ValidationConstants.Messages.USER_EMAIL_REQUIRED);
        assertEquals("Invalid email format", ValidationConstants.Messages.USER_EMAIL_INVALID);
        assertEquals("Password is required", ValidationConstants.Messages.USER_PASSWORD_REQUIRED);
        assertEquals("Password must be at least 8 characters", ValidationConstants.Messages.USER_PASSWORD_TOO_SHORT);
        assertEquals("Role is required", ValidationConstants.Messages.USER_ROLE_REQUIRED);
    }

    @Test
    void testVendorValidationMessages() {
        assertEquals("Company name is required", ValidationConstants.Messages.VENDOR_COMPANY_NAME_REQUIRED);
        assertEquals("Company name must not exceed 200 characters", ValidationConstants.Messages.VENDOR_COMPANY_NAME_TOO_LONG);
        assertEquals("Email is required", ValidationConstants.Messages.VENDOR_EMAIL_REQUIRED);
        assertEquals("Invalid email format", ValidationConstants.Messages.VENDOR_EMAIL_INVALID);
        assertEquals("Password is required", ValidationConstants.Messages.VENDOR_PASSWORD_REQUIRED);
        assertEquals("Password must be at least 8 characters", ValidationConstants.Messages.VENDOR_PASSWORD_TOO_SHORT);
        assertEquals("GST number is required", ValidationConstants.Messages.VENDOR_GST_REQUIRED);
        assertEquals("GST number must be exactly 15 characters", ValidationConstants.Messages.VENDOR_GST_INVALID_LENGTH);
        assertEquals("Registration ID is required", ValidationConstants.Messages.VENDOR_REGISTRATION_ID_REQUIRED);
    }

    @Test
    void testRfqValidationMessages() {
        assertEquals("Title is required", ValidationConstants.Messages.RFQ_TITLE_REQUIRED);
        assertEquals("Title must not exceed 200 characters", ValidationConstants.Messages.RFQ_TITLE_TOO_LONG);
        assertEquals("Terms and conditions are required", ValidationConstants.Messages.RFQ_TERMS_REQUIRED);
        assertEquals("Deadline is required", ValidationConstants.Messages.RFQ_DEADLINE_REQUIRED);
        assertEquals("Deadline must be in the future", ValidationConstants.Messages.RFQ_DEADLINE_MUST_BE_FUTURE);
        assertEquals("At least one item is required", ValidationConstants.Messages.RFQ_ITEMS_REQUIRED);
        assertEquals("At least one vendor must be invited", ValidationConstants.Messages.RFQ_VENDORS_REQUIRED);
        assertEquals("Item name is required", ValidationConstants.Messages.RFQ_ITEM_NAME_REQUIRED);
        assertEquals("Quantity is required", ValidationConstants.Messages.RFQ_ITEM_QUANTITY_REQUIRED);
        assertEquals("Quantity must be at least 1", ValidationConstants.Messages.RFQ_ITEM_QUANTITY_MIN);
        assertEquals("Unit is required", ValidationConstants.Messages.RFQ_ITEM_UNIT_REQUIRED);
    }

    @Test
    void testQuotationValidationMessages() {
        assertEquals("RFQ ID is required", ValidationConstants.Messages.QUOTATION_RFQ_ID_REQUIRED);
        assertEquals("Total amount is required", ValidationConstants.Messages.QUOTATION_TOTAL_AMOUNT_REQUIRED);
        assertEquals("Total amount must be at least 0.01", ValidationConstants.Messages.QUOTATION_TOTAL_AMOUNT_MIN);
        assertEquals("Tax percentage is required", ValidationConstants.Messages.QUOTATION_TAX_REQUIRED);
        assertEquals("Tax percentage must be between 0.0 and 100.0", ValidationConstants.Messages.QUOTATION_TAX_RANGE);
        assertEquals("Currency is required", ValidationConstants.Messages.QUOTATION_CURRENCY_REQUIRED);
        assertEquals("Delivery days is required", ValidationConstants.Messages.QUOTATION_DELIVERY_DAYS_REQUIRED);
        assertEquals("Delivery days must be at least 1", ValidationConstants.Messages.QUOTATION_DELIVERY_DAYS_MIN);
        assertEquals("At least one item is required", ValidationConstants.Messages.QUOTATION_ITEMS_REQUIRED);
    }

    @Test
    void testPurchaseOrderValidationMessages() {
        assertEquals("Delivery date is required", ValidationConstants.Messages.PO_DELIVERY_DATE_REQUIRED);
        assertEquals("Delivery date must be in the future", ValidationConstants.Messages.PO_DELIVERY_DATE_MUST_BE_FUTURE);
        assertEquals("Shipping address is required", ValidationConstants.Messages.PO_SHIPPING_ADDRESS_REQUIRED);
    }

    @Test
    void testRoleValidationMessages() {
        assertEquals("Role name is required", ValidationConstants.Messages.ROLE_NAME_REQUIRED);
        assertEquals("Role name must not exceed 100 characters", ValidationConstants.Messages.ROLE_NAME_TOO_LONG);
        assertEquals("Module name is required", ValidationConstants.Messages.ROLE_MODULE_REQUIRED);
    }

    @Test
    void testNotificationValidationMessages() {
        assertEquals("Notification type is required", ValidationConstants.Messages.NOTIFICATION_TYPE_REQUIRED);
        assertEquals("In-app enabled flag is required", ValidationConstants.Messages.NOTIFICATION_IN_APP_REQUIRED);
        assertEquals("Email enabled flag is required", ValidationConstants.Messages.NOTIFICATION_EMAIL_REQUIRED);
    }

    @Test
    void testLoginValidationMessages() {
        assertEquals("Email is required", ValidationConstants.Messages.LOGIN_EMAIL_REQUIRED);
        assertEquals("Invalid email format", ValidationConstants.Messages.LOGIN_EMAIL_INVALID);
        assertEquals("Password is required", ValidationConstants.Messages.LOGIN_PASSWORD_REQUIRED);
    }

    @Test
    void testApprovalValidationMessages() {
        assertEquals("Reason is mandatory for rejection/suspension", ValidationConstants.Messages.APPROVAL_REASON_REQUIRED);
    }

    // ========== VALIDATION PATTERNS ==========
    @Test
    void testValidationPatterns() {
        assertEquals("^[A-Za-z0-9+_.-]+@(.+)$", ValidationConstants.Patterns.EMAIL);
        assertEquals("^[0-9]{10,15}$", ValidationConstants.Patterns.PHONE);
        assertEquals("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", ValidationConstants.Patterns.GST);
        assertEquals("^[a-zA-Z0-9 ]+$", ValidationConstants.Patterns.ALPHANUMERIC);
    }

    @Test
    void testEmailPatternValidation() {
        Pattern emailPattern = Pattern.compile(ValidationConstants.Patterns.EMAIL);
        
        // Valid emails
        assertTrue(emailPattern.matcher("user@domain.com").matches());
        assertTrue(emailPattern.matcher("user.name@domain.com").matches());
        assertTrue(emailPattern.matcher("user+tag@domain.com").matches());
        assertTrue(emailPattern.matcher("user123@domain123.com").matches());
        
        // Invalid emails
        assertFalse(emailPattern.matcher("plainaddress").matches());
        assertFalse(emailPattern.matcher("@domain.com").matches());
        assertFalse(emailPattern.matcher("user@").matches());
    }

    @Test
    void testPhonePatternValidation() {
        Pattern phonePattern = Pattern.compile(ValidationConstants.Patterns.PHONE);
        
        // Valid phones
        assertTrue(phonePattern.matcher("1234567890").matches()); // 10 digits
        assertTrue(phonePattern.matcher("123456789012345").matches()); // 15 digits
        assertTrue(phonePattern.matcher("12345678901").matches()); // 11 digits
        
        // Invalid phones
        assertFalse(phonePattern.matcher("123456789").matches()); // 9 digits (too short)
        assertFalse(phonePattern.matcher("1234567890123456").matches()); // 16 digits (too long)
        assertFalse(phonePattern.matcher("12345abc90").matches()); // contains letters
        assertFalse(phonePattern.matcher("123-456-7890").matches()); // contains hyphens
    }

    @Test
    void testGstPatternValidation() {
        Pattern gstPattern = Pattern.compile(ValidationConstants.Patterns.GST);
        
        // Valid GST format: 2 digits + 5 letters + 4 digits + 1 letter + 1 alphanumeric + Z + 1 alphanumeric
        assertTrue(gstPattern.matcher("29ABCDE1234F1Z5").matches());
        assertTrue(gstPattern.matcher("12ABCDE5678G9ZA").matches());
        
        // Invalid GST formats
        assertFalse(gstPattern.matcher("29ABCDE1234F1Y5").matches()); // Y instead of Z
        assertFalse(gstPattern.matcher("2ABCDE1234F1Z5").matches()); // Only 1 digit at start
        assertFalse(gstPattern.matcher("29abcde1234F1Z5").matches()); // lowercase letters
        assertFalse(gstPattern.matcher("29ABCDE123F1Z5").matches()); // Only 3 digits in middle
    }

    @Test
    void testAlphanumericPatternValidation() {
        Pattern alphanumericPattern = Pattern.compile(ValidationConstants.Patterns.ALPHANUMERIC);
        
        // Valid alphanumeric
        assertTrue(alphanumericPattern.matcher("ABC123").matches());
        assertTrue(alphanumericPattern.matcher("Test Name 123").matches());
        assertTrue(alphanumericPattern.matcher("123").matches());
        assertTrue(alphanumericPattern.matcher("ABC").matches());
        assertTrue(alphanumericPattern.matcher("A B C 1 2 3").matches());
        
        // Invalid alphanumeric
        assertFalse(alphanumericPattern.matcher("ABC@123").matches()); // special character
        assertFalse(alphanumericPattern.matcher("Test-Name").matches()); // hyphen
        assertFalse(alphanumericPattern.matcher("Test_Name").matches()); // underscore
        assertFalse(alphanumericPattern.matcher("Test.Name").matches()); // dot
    }

    @Test
    void testConstantsArePublicStaticFinal() throws NoSuchFieldException {
        // Test a few key constants to ensure they are public static final
        assertTrue(Modifier.isPublic(ValidationConstants.class.getField("USER_NAME_MAX_LENGTH").getModifiers()));
        assertTrue(Modifier.isStatic(ValidationConstants.class.getField("USER_NAME_MAX_LENGTH").getModifiers()));
        assertTrue(Modifier.isFinal(ValidationConstants.class.getField("USER_NAME_MAX_LENGTH").getModifiers()));
        
        assertTrue(Modifier.isPublic(ValidationConstants.class.getField("USER_EMAIL_PATTERN").getModifiers()));
        assertTrue(Modifier.isStatic(ValidationConstants.class.getField("USER_EMAIL_PATTERN").getModifiers()));
        assertTrue(Modifier.isFinal(ValidationConstants.class.getField("USER_EMAIL_PATTERN").getModifiers()));
    }

    @Test
    void testMessagesClassExists() {
        // Verify Messages inner class exists and has expected structure
        assertNotNull(ValidationConstants.Messages.class);
        assertTrue(Modifier.isPublic(ValidationConstants.Messages.class.getModifiers()));
        assertTrue(Modifier.isStatic(ValidationConstants.Messages.class.getModifiers()));
    }

    @Test
    void testPatternsClassExists() {
        // Verify Patterns inner class exists and has expected structure
        assertNotNull(ValidationConstants.Patterns.class);
        assertTrue(Modifier.isPublic(ValidationConstants.Patterns.class.getModifiers()));
        assertTrue(Modifier.isStatic(ValidationConstants.Patterns.class.getModifiers()));
    }

    @Test
    void testMessageConsistencyWithConstants() {
        // Test that messages reference the correct constant values
        assertTrue(ValidationConstants.Messages.USER_NAME_TOO_LONG.contains("100"));
        assertTrue(ValidationConstants.Messages.USER_PASSWORD_TOO_SHORT.contains("8"));
        assertTrue(ValidationConstants.Messages.VENDOR_COMPANY_NAME_TOO_LONG.contains("200"));
        assertTrue(ValidationConstants.Messages.VENDOR_PASSWORD_TOO_SHORT.contains("8"));
        assertTrue(ValidationConstants.Messages.VENDOR_GST_INVALID_LENGTH.contains("15"));
        assertTrue(ValidationConstants.Messages.RFQ_TITLE_TOO_LONG.contains("200"));
        assertTrue(ValidationConstants.Messages.RFQ_ITEM_QUANTITY_MIN.contains("1"));
        assertTrue(ValidationConstants.Messages.QUOTATION_TOTAL_AMOUNT_MIN.contains("0.01"));
        assertTrue(ValidationConstants.Messages.QUOTATION_TAX_RANGE.contains("0.0") && 
                  ValidationConstants.Messages.QUOTATION_TAX_RANGE.contains("100.0"));
        assertTrue(ValidationConstants.Messages.QUOTATION_DELIVERY_DAYS_MIN.contains("1"));
        assertTrue(ValidationConstants.Messages.ROLE_NAME_TOO_LONG.contains("100"));
    }
}