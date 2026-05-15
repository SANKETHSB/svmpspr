package com.infosys.svpms.utility;

/**
 * Centralized validation constants and rules for the SVPMS application
 * All backend validation rules are defined here for consistency
 */
public class ValidationConstants {

    // ========== USER VALIDATION ==========
    public static final int USER_NAME_MAX_LENGTH = 100;
    public static final int USER_PASSWORD_MIN_LENGTH = 8;
    public static final String USER_EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    
    // ========== VENDOR VALIDATION ==========
    public static final int VENDOR_COMPANY_NAME_MAX_LENGTH = 200;
    public static final int VENDOR_GST_LENGTH = 15;
    public static final int VENDOR_PHONE_MAX_LENGTH = 15;
    public static final int VENDOR_ADDRESS_MAX_LENGTH = 500;
    public static final int VENDOR_PASSWORD_MIN_LENGTH = 8;
    
    // ========== RFQ VALIDATION ==========
    public static final int RFQ_TITLE_MAX_LENGTH = 200;
    public static final int RFQ_DESCRIPTION_MAX_LENGTH = 2000;
    public static final int RFQ_ITEM_NAME_MAX_LENGTH = 200;
    public static final int RFQ_ITEM_DESCRIPTION_MAX_LENGTH = 500;
    public static final int RFQ_ITEM_UNIT_MAX_LENGTH = 50;
    public static final int RFQ_ITEM_MIN_QUANTITY = 1;
    
    // ========== QUOTATION VALIDATION ==========
    public static final String QUOTATION_MIN_AMOUNT = "0.01";
    public static final String QUOTATION_MIN_TAX = "0.0";
    public static final String QUOTATION_MAX_TAX = "100.0";
    public static final int QUOTATION_CURRENCY_MAX_LENGTH = 10;
    public static final int QUOTATION_MIN_DELIVERY_DAYS = 1;
    public static final int QUOTATION_NOTES_MAX_LENGTH = 1000;
    public static final int QUOTATION_EVALUATION_COMMENT_MIN_LENGTH = 10;
    public static final int QUOTATION_EVALUATION_COMMENT_MAX_LENGTH = 1000;
    public static final String QUOTATION_MIN_SCORE = "0.0";
    public static final String QUOTATION_MAX_SCORE = "100.0";
    
    // ========== PURCHASE ORDER VALIDATION ==========
    public static final int PO_SHIPPING_ADDRESS_MAX_LENGTH = 500;
    public static final int PO_PAYMENT_TERMS_MAX_LENGTH = 500;
    public static final int PO_SPECIAL_INSTRUCTIONS_MAX_LENGTH = 1000;
    
    // ========== ROLE VALIDATION ==========
    public static final int ROLE_NAME_MAX_LENGTH = 100;
    public static final int ROLE_DESCRIPTION_MAX_LENGTH = 500;
    
    // ========== NOTIFICATION VALIDATION ==========
    public static final int NOTIFICATION_TITLE_MAX_LENGTH = 200;
    public static final int NOTIFICATION_MESSAGE_MAX_LENGTH = 1000;
    
    // ========== VALIDATION MESSAGES ==========
    public static class Messages {
        // User messages
        public static final String USER_NAME_REQUIRED = "Name is required";
        public static final String USER_NAME_TOO_LONG = "Name must not exceed " + USER_NAME_MAX_LENGTH + " characters";
        public static final String USER_EMAIL_REQUIRED = "Email is required";
        public static final String USER_EMAIL_INVALID = "Invalid email format";
        public static final String USER_PASSWORD_REQUIRED = "Password is required";
        public static final String USER_PASSWORD_TOO_SHORT = "Password must be at least " + USER_PASSWORD_MIN_LENGTH + " characters";
        public static final String USER_ROLE_REQUIRED = "Role is required";
        
        // Vendor messages
        public static final String VENDOR_COMPANY_NAME_REQUIRED = "Company name is required";
        public static final String VENDOR_COMPANY_NAME_TOO_LONG = "Company name must not exceed " + VENDOR_COMPANY_NAME_MAX_LENGTH + " characters";
        public static final String VENDOR_EMAIL_REQUIRED = "Email is required";
        public static final String VENDOR_EMAIL_INVALID = "Invalid email format";
        public static final String VENDOR_PASSWORD_REQUIRED = "Password is required";
        public static final String VENDOR_PASSWORD_TOO_SHORT = "Password must be at least " + VENDOR_PASSWORD_MIN_LENGTH + " characters";
        public static final String VENDOR_GST_REQUIRED = "GST number is required";
        public static final String VENDOR_GST_INVALID_LENGTH = "GST number must be exactly " + VENDOR_GST_LENGTH + " characters";
        public static final String VENDOR_REGISTRATION_ID_REQUIRED = "Registration ID is required";
        
        // RFQ messages
        public static final String RFQ_TITLE_REQUIRED = "Title is required";
        public static final String RFQ_TITLE_TOO_LONG = "Title must not exceed " + RFQ_TITLE_MAX_LENGTH + " characters";
        public static final String RFQ_TERMS_REQUIRED = "Terms and conditions are required";
        public static final String RFQ_DEADLINE_REQUIRED = "Deadline is required";
        public static final String RFQ_DEADLINE_MUST_BE_FUTURE = "Deadline must be in the future";
        public static final String RFQ_ITEMS_REQUIRED = "At least one item is required";
        public static final String RFQ_VENDORS_REQUIRED = "At least one vendor must be invited";
        public static final String RFQ_ITEM_NAME_REQUIRED = "Item name is required";
        public static final String RFQ_ITEM_QUANTITY_REQUIRED = "Quantity is required";
        public static final String RFQ_ITEM_QUANTITY_MIN = "Quantity must be at least " + RFQ_ITEM_MIN_QUANTITY;
        public static final String RFQ_ITEM_UNIT_REQUIRED = "Unit is required";
        
        // Quotation messages
        public static final String QUOTATION_RFQ_ID_REQUIRED = "RFQ ID is required";
        public static final String QUOTATION_TOTAL_AMOUNT_REQUIRED = "Total amount is required";
        public static final String QUOTATION_TOTAL_AMOUNT_MIN = "Total amount must be at least " + QUOTATION_MIN_AMOUNT;
        public static final String QUOTATION_TAX_REQUIRED = "Tax percentage is required";
        public static final String QUOTATION_TAX_RANGE = "Tax percentage must be between " + QUOTATION_MIN_TAX + " and " + QUOTATION_MAX_TAX;
        public static final String QUOTATION_CURRENCY_REQUIRED = "Currency is required";
        public static final String QUOTATION_DELIVERY_DAYS_REQUIRED = "Delivery days is required";
        public static final String QUOTATION_DELIVERY_DAYS_MIN = "Delivery days must be at least " + QUOTATION_MIN_DELIVERY_DAYS;
        public static final String QUOTATION_ITEMS_REQUIRED = "At least one item is required";
        public static final String QUOTATION_ITEM_RFQ_ITEM_ID_REQUIRED = "RFQ item ID is required";
        public static final String QUOTATION_ITEM_UNIT_PRICE_REQUIRED = "Unit price is required";
        public static final String QUOTATION_ITEM_UNIT_PRICE_MIN = "Unit price must be at least " + QUOTATION_MIN_AMOUNT;
        public static final String QUOTATION_EVALUATION_SCORE_REQUIRED = "Evaluation score is required";
        public static final String QUOTATION_EVALUATION_SCORE_RANGE = "Score must be between " + QUOTATION_MIN_SCORE + " and " + QUOTATION_MAX_SCORE;
        public static final String QUOTATION_EVALUATION_COMMENT_REQUIRED = "Evaluation comment is required";
        public static final String QUOTATION_EVALUATION_COMMENT_LENGTH = "Comment must be between " + QUOTATION_EVALUATION_COMMENT_MIN_LENGTH + " and " + QUOTATION_EVALUATION_COMMENT_MAX_LENGTH + " characters";
        public static final String QUOTATION_AWARD_ID_REQUIRED = "Quotation ID is required for award";
        public static final String QUOTATION_AWARD_REASON_REQUIRED = "Award reason is required";
        public static final String QUOTATION_AWARD_CONFIRMATION_REQUIRED = "Award confirmation is required";
        
        // Purchase Order messages
        public static final String PO_DELIVERY_DATE_REQUIRED = "Delivery date is required";
        public static final String PO_DELIVERY_DATE_MUST_BE_FUTURE = "Delivery date must be in the future";
        public static final String PO_SHIPPING_ADDRESS_REQUIRED = "Shipping address is required";
        
        // Role messages
        public static final String ROLE_NAME_REQUIRED = "Role name is required";
        public static final String ROLE_NAME_TOO_LONG = "Role name must not exceed " + ROLE_NAME_MAX_LENGTH + " characters";
        public static final String ROLE_MODULE_REQUIRED = "Module name is required";
        
        // Notification messages
        public static final String NOTIFICATION_TYPE_REQUIRED = "Notification type is required";
        public static final String NOTIFICATION_IN_APP_REQUIRED = "In-app enabled flag is required";
        public static final String NOTIFICATION_EMAIL_REQUIRED = "Email enabled flag is required";
        
        // Login messages
        public static final String LOGIN_EMAIL_REQUIRED = "Email is required";
        public static final String LOGIN_EMAIL_INVALID = "Invalid email format";
        public static final String LOGIN_PASSWORD_REQUIRED = "Password is required";
        
        // Approval messages
        public static final String APPROVAL_REASON_REQUIRED = "Reason is mandatory for rejection/suspension";
    }
    
    // ========== VALIDATION PATTERNS ==========
    public static class Patterns {
        public static final String EMAIL = "^[A-Za-z0-9+_.-]+@(.+)$";
        public static final String PHONE = "^[0-9]{10,15}$";
        public static final String GST = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$";
        public static final String ALPHANUMERIC = "^[a-zA-Z0-9 ]+$";
    }
    
    private ValidationConstants() {
        // Utility class, prevent instantiation
    }
}
