/**
 * Centralized validation utilities for the SVPMS frontend
 * All frontend validation rules are defined here for consistency
 */

// ========== VALIDATION CONSTANTS ==========
export const VALIDATION_CONSTANTS = {
  // User validation
  USER_NAME_MAX_LENGTH: 100,
  USER_PASSWORD_MIN_LENGTH: 8,
  
  // Vendor validation
  VENDOR_COMPANY_NAME_MAX_LENGTH: 200,
  VENDOR_GST_LENGTH: 15,
  VENDOR_PHONE_MAX_LENGTH: 15,
  VENDOR_ADDRESS_MAX_LENGTH: 500,
  VENDOR_PASSWORD_MIN_LENGTH: 8,
  
  // RFQ validation
  RFQ_TITLE_MAX_LENGTH: 200,
  RFQ_DESCRIPTION_MAX_LENGTH: 2000,
  RFQ_ITEM_NAME_MAX_LENGTH: 200,
  RFQ_ITEM_DESCRIPTION_MAX_LENGTH: 500,
  RFQ_ITEM_UNIT_MAX_LENGTH: 50,
  RFQ_ITEM_MIN_QUANTITY: 1,
  
  // Quotation validation
  QUOTATION_MIN_AMOUNT: 0.01,
  QUOTATION_MIN_TAX: 0.0,
  QUOTATION_MAX_TAX: 100.0,
  QUOTATION_CURRENCY_MAX_LENGTH: 10,
  QUOTATION_MIN_DELIVERY_DAYS: 1,
  QUOTATION_NOTES_MAX_LENGTH: 1000,
  QUOTATION_EVALUATION_COMMENT_MIN_LENGTH: 10,
  QUOTATION_EVALUATION_COMMENT_MAX_LENGTH: 1000,
  QUOTATION_MIN_SCORE: 0.0,
  QUOTATION_MAX_SCORE: 100.0,
  
  // Purchase Order validation
  PO_SHIPPING_ADDRESS_MAX_LENGTH: 500,
  PO_PAYMENT_TERMS_MAX_LENGTH: 500,
  PO_SPECIAL_INSTRUCTIONS_MAX_LENGTH: 1000,
  
  // Role validation
  ROLE_NAME_MAX_LENGTH: 100,
  ROLE_DESCRIPTION_MAX_LENGTH: 500,
  
  // Notification validation
  NOTIFICATION_TITLE_MAX_LENGTH: 200,
  NOTIFICATION_MESSAGE_MAX_LENGTH: 1000,
};

// ========== VALIDATION PATTERNS ==========
export const VALIDATION_PATTERNS = {
  EMAIL: /^[A-Za-z0-9+_.-]+@(.+)$/,
  PHONE: /^[0-9]{10,15}$/,
  GST: /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/,
  ALPHANUMERIC: /^[a-zA-Z0-9 ]+$/,
  DECIMAL: /^\d+(\.\d{1,2})?$/,
};

// ========== VALIDATION MESSAGES ==========
export const VALIDATION_MESSAGES = {
  // User messages
  USER_NAME_REQUIRED: 'Name is required',
  USER_NAME_TOO_LONG: `Name must not exceed ${VALIDATION_CONSTANTS.USER_NAME_MAX_LENGTH} characters`,
  USER_EMAIL_REQUIRED: 'Email is required',
  USER_EMAIL_INVALID: 'Invalid email format',
  USER_PASSWORD_REQUIRED: 'Password is required',
  USER_PASSWORD_TOO_SHORT: `Password must be at least ${VALIDATION_CONSTANTS.USER_PASSWORD_MIN_LENGTH} characters`,
  USER_ROLE_REQUIRED: 'Role is required',
  
  // Vendor messages
  VENDOR_COMPANY_NAME_REQUIRED: 'Company name is required',
  VENDOR_COMPANY_NAME_TOO_LONG: `Company name must not exceed ${VALIDATION_CONSTANTS.VENDOR_COMPANY_NAME_MAX_LENGTH} characters`,
  VENDOR_EMAIL_REQUIRED: 'Email is required',
  VENDOR_EMAIL_INVALID: 'Invalid email format',
  VENDOR_PASSWORD_REQUIRED: 'Password is required',
  VENDOR_PASSWORD_TOO_SHORT: `Password must be at least ${VALIDATION_CONSTANTS.VENDOR_PASSWORD_MIN_LENGTH} characters`,
  VENDOR_GST_REQUIRED: 'GST number is required',
  VENDOR_GST_INVALID_LENGTH: `GST number must be exactly ${VALIDATION_CONSTANTS.VENDOR_GST_LENGTH} characters`,
  VENDOR_REGISTRATION_ID_REQUIRED: 'Registration ID is required',
  
  // RFQ messages
  RFQ_TITLE_REQUIRED: 'Title is required',
  RFQ_TITLE_TOO_LONG: `Title must not exceed ${VALIDATION_CONSTANTS.RFQ_TITLE_MAX_LENGTH} characters`,
  RFQ_TERMS_REQUIRED: 'Terms and conditions are required',
  RFQ_DEADLINE_REQUIRED: 'Deadline is required',
  RFQ_DEADLINE_MUST_BE_FUTURE: 'Deadline must be in the future',
  RFQ_ITEMS_REQUIRED: 'At least one item is required',
  RFQ_VENDORS_REQUIRED: 'At least one vendor must be invited',
  RFQ_ITEM_NAME_REQUIRED: 'Item name is required',
  RFQ_ITEM_QUANTITY_REQUIRED: 'Quantity is required',
  RFQ_ITEM_QUANTITY_MIN: `Quantity must be at least ${VALIDATION_CONSTANTS.RFQ_ITEM_MIN_QUANTITY}`,
  RFQ_ITEM_UNIT_REQUIRED: 'Unit is required',
  
  // Quotation messages
  QUOTATION_RFQ_ID_REQUIRED: 'RFQ ID is required',
  QUOTATION_TOTAL_AMOUNT_REQUIRED: 'Total amount is required',
  QUOTATION_TOTAL_AMOUNT_MIN: `Total amount must be at least ${VALIDATION_CONSTANTS.QUOTATION_MIN_AMOUNT}`,
  QUOTATION_TAX_REQUIRED: 'Tax percentage is required',
  QUOTATION_TAX_RANGE: `Tax percentage must be between ${VALIDATION_CONSTANTS.QUOTATION_MIN_TAX} and ${VALIDATION_CONSTANTS.QUOTATION_MAX_TAX}`,
  QUOTATION_CURRENCY_REQUIRED: 'Currency is required',
  QUOTATION_DELIVERY_DAYS_REQUIRED: 'Delivery days is required',
  QUOTATION_DELIVERY_DAYS_MIN: `Delivery days must be at least ${VALIDATION_CONSTANTS.QUOTATION_MIN_DELIVERY_DAYS}`,
  QUOTATION_ITEMS_REQUIRED: 'At least one item is required',
  
  // Purchase Order messages
  PO_DELIVERY_DATE_REQUIRED: 'Delivery date is required',
  PO_DELIVERY_DATE_MUST_BE_FUTURE: 'Delivery date must be in the future',
  PO_SHIPPING_ADDRESS_REQUIRED: 'Shipping address is required',
  
  // Login messages
  LOGIN_EMAIL_REQUIRED: 'Email is required',
  LOGIN_EMAIL_INVALID: 'Invalid email format',
  LOGIN_PASSWORD_REQUIRED: 'Password is required',
};

// ========== VALIDATION FUNCTIONS ==========

/**
 * Validate email format
 */
export const isValidEmail = (email: string): boolean => {
  return VALIDATION_PATTERNS.EMAIL.test(email);
};

/**
 * Validate password strength
 */
export const isValidPassword = (password: string): boolean => {
  return password.length >= VALIDATION_CONSTANTS.USER_PASSWORD_MIN_LENGTH;
};

/**
 * Validate phone number
 */
export const isValidPhone = (phone: string): boolean => {
  return VALIDATION_PATTERNS.PHONE.test(phone);
};

/**
 * Validate GST number
 */
export const isValidGST = (gst: string): boolean => {
  return gst.length === VALIDATION_CONSTANTS.VENDOR_GST_LENGTH;
};

/**
 * Validate required field
 */
export const isRequired = (value: any): boolean => {
  if (typeof value === 'string') {
    return value.trim().length > 0;
  }
  return value !== null && value !== undefined;
};

/**
 * Validate max length
 */
export const isMaxLength = (value: string, maxLength: number): boolean => {
  return value.length <= maxLength;
};

/**
 * Validate min length
 */
export const isMinLength = (value: string, minLength: number): boolean => {
  return value.length >= minLength;
};

/**
 * Validate number range
 */
export const isInRange = (value: number, min: number, max: number): boolean => {
  return value >= min && value <= max;
};

/**
 * Validate positive number
 */
export const isPositive = (value: number): boolean => {
  return value > 0;
};

/**
 * Validate decimal format
 */
export const isValidDecimal = (value: string): boolean => {
  return VALIDATION_PATTERNS.DECIMAL.test(value);
};

/**
 * Validate future date
 */
export const isFutureDate = (date: Date | string): boolean => {
  const inputDate = typeof date === 'string' ? new Date(date) : date;
  return inputDate > new Date();
};

/**
 * Validate alphanumeric
 */
export const isAlphanumeric = (value: string): boolean => {
  return VALIDATION_PATTERNS.ALPHANUMERIC.test(value);
};

// ========== FORM VALIDATION HELPERS ==========

/**
 * Validate user form
 */
export const validateUserForm = (data: {
  name: string;
  email: string;
  password?: string;
  role: string;
}): { [key: string]: string } => {
  const errors: { [key: string]: string } = {};

  if (!isRequired(data.name)) {
    errors.name = VALIDATION_MESSAGES.USER_NAME_REQUIRED;
  } else if (!isMaxLength(data.name, VALIDATION_CONSTANTS.USER_NAME_MAX_LENGTH)) {
    errors.name = VALIDATION_MESSAGES.USER_NAME_TOO_LONG;
  }

  if (!isRequired(data.email)) {
    errors.email = VALIDATION_MESSAGES.USER_EMAIL_REQUIRED;
  } else if (!isValidEmail(data.email)) {
    errors.email = VALIDATION_MESSAGES.USER_EMAIL_INVALID;
  }

  if (data.password !== undefined) {
    if (!isRequired(data.password)) {
      errors.password = VALIDATION_MESSAGES.USER_PASSWORD_REQUIRED;
    } else if (!isValidPassword(data.password)) {
      errors.password = VALIDATION_MESSAGES.USER_PASSWORD_TOO_SHORT;
    }
  }

  if (!isRequired(data.role)) {
    errors.role = VALIDATION_MESSAGES.USER_ROLE_REQUIRED;
  }

  return errors;
};

/**
 * Validate vendor registration form
 */
export const validateVendorForm = (data: {
  companyName: string;
  email: string;
  password: string;
  gstNumber: string;
  registrationId: string;
}): { [key: string]: string } => {
  const errors: { [key: string]: string } = {};

  if (!isRequired(data.companyName)) {
    errors.companyName = VALIDATION_MESSAGES.VENDOR_COMPANY_NAME_REQUIRED;
  } else if (!isMaxLength(data.companyName, VALIDATION_CONSTANTS.VENDOR_COMPANY_NAME_MAX_LENGTH)) {
    errors.companyName = VALIDATION_MESSAGES.VENDOR_COMPANY_NAME_TOO_LONG;
  }

  if (!isRequired(data.email)) {
    errors.email = VALIDATION_MESSAGES.VENDOR_EMAIL_REQUIRED;
  } else if (!isValidEmail(data.email)) {
    errors.email = VALIDATION_MESSAGES.VENDOR_EMAIL_INVALID;
  }

  if (!isRequired(data.password)) {
    errors.password = VALIDATION_MESSAGES.VENDOR_PASSWORD_REQUIRED;
  } else if (!isValidPassword(data.password)) {
    errors.password = VALIDATION_MESSAGES.VENDOR_PASSWORD_TOO_SHORT;
  }

  if (!isRequired(data.gstNumber)) {
    errors.gstNumber = VALIDATION_MESSAGES.VENDOR_GST_REQUIRED;
  } else if (!isValidGST(data.gstNumber)) {
    errors.gstNumber = VALIDATION_MESSAGES.VENDOR_GST_INVALID_LENGTH;
  }

  if (!isRequired(data.registrationId)) {
    errors.registrationId = VALIDATION_MESSAGES.VENDOR_REGISTRATION_ID_REQUIRED;
  }

  return errors;
};

/**
 * Validate login form
 */
export const validateLoginForm = (data: {
  email: string;
  password: string;
}): { [key: string]: string } => {
  const errors: { [key: string]: string } = {};

  if (!isRequired(data.email)) {
    errors.email = VALIDATION_MESSAGES.LOGIN_EMAIL_REQUIRED;
  } else if (!isValidEmail(data.email)) {
    errors.email = VALIDATION_MESSAGES.LOGIN_EMAIL_INVALID;
  }

  if (!isRequired(data.password)) {
    errors.password = VALIDATION_MESSAGES.LOGIN_PASSWORD_REQUIRED;
  }

  return errors;
};

export default {
  VALIDATION_CONSTANTS,
  VALIDATION_PATTERNS,
  VALIDATION_MESSAGES,
  isValidEmail,
  isValidPassword,
  isValidPhone,
  isValidGST,
  isRequired,
  isMaxLength,
  isMinLength,
  isInRange,
  isPositive,
  isValidDecimal,
  isFutureDate,
  isAlphanumeric,
  validateUserForm,
  validateVendorForm,
  validateLoginForm,
};
