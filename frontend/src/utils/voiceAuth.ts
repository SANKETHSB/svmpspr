// Mock credentials for demonstration
// In production, these should come from a secure backend
const ADMIN_CREDENTIALS = {
  username: 'admin',
  password: 'admin123',
  role: 'ADMIN',
};

const MANAGER_CREDENTIALS = {
  username: 'manager',
  password: 'manager123',
  role: 'PROCUREMENT_MANAGER',
};

const COMPLIANCE_CREDENTIALS = {
  username: 'compliance',
  password: 'compliance123',
  role: 'COMPLIANCE_OFFICER',
};

// Mock vendor database with emails
const VENDOR_DATABASE = [
  { email: 'vendor@example.com', name: 'Vendor One' },
  { email: 'test@vendor.com', name: 'Test Vendor' },
  { email: 'supplier@company.com', name: 'Supplier Company' },
];

export interface VoiceAuthResult {
  success: boolean;
  message: string;
  user?: {
    username: string;
    role: string;
    email?: string;
    accessToken: string;
  };
}

// Detect role from voice input
export const detectRoleFromVoice = (transcript: string): string | null => {
  const lower = transcript.toLowerCase();

  if (lower.includes('admin') && lower.includes('buddy')) {
    return 'ADMIN';
  }
  if (lower.includes('manager') && lower.includes('buddy')) {
    return 'PROCUREMENT_MANAGER';
  }
  if (lower.includes('compliance') && lower.includes('buddy')) {
    return 'COMPLIANCE_OFFICER';
  }
  if (lower.includes('vendor') && lower.includes('login')) {
    return 'VENDOR';
  }

  return null;
};

// Extract password from voice input
export const extractPasswordFromVoice = (transcript: string): string => {
  // Remove common filler words and punctuation
  return transcript.toLowerCase().trim().replace(/[.,!?;:]/g, '');
};

// Extract email from voice input
export const extractEmailFromVoice = (transcript: string): string => {
  // Try to extract email-like pattern
  const emailRegex = /[\w\.-]+@[\w\.-]+\.\w+/;
  const match = transcript.match(emailRegex);

  if (match) {
    return match[0];
  }

  // Try to convert spoken email to written format
  // e.g., "john at example dot com" -> "john@example.com"
  let converted = transcript
    .toLowerCase()
    .replace(/\s+at\s+/gi, '@')
    .replace(/\s+dot\s+/gi, '.');

  return converted.trim();
};

// Verify admin password
export const verifyAdminPassword = async (password: string): Promise<VoiceAuthResult> => {
  // Add slight delay to simulate backend call
  await new Promise((resolve) => setTimeout(resolve, 500));

  const extractedPassword = extractPasswordFromVoice(password);

  if (extractedPassword === ADMIN_CREDENTIALS.password) {
    return {
      success: true,
      message: 'Admin login successful! Welcome to your dashboard.',
      user: {
        username: ADMIN_CREDENTIALS.username,
        role: ADMIN_CREDENTIALS.role,
        accessToken: 'admin_token_' + Date.now(),
      },
    };
  }

  return {
    success: false,
    message: 'Incorrect password. Please try again.',
  };
};

// Verify manager password
export const verifyManagerPassword = async (password: string): Promise<VoiceAuthResult> => {
  await new Promise((resolve) => setTimeout(resolve, 500));

  const extractedPassword = extractPasswordFromVoice(password);

  if (extractedPassword === MANAGER_CREDENTIALS.password) {
    return {
      success: true,
      message: 'Manager login successful! Welcome to your dashboard.',
      user: {
        username: MANAGER_CREDENTIALS.username,
        role: MANAGER_CREDENTIALS.role,
        accessToken: 'manager_token_' + Date.now(),
      },
    };
  }

  return {
    success: false,
    message: 'Incorrect password. Please try again.',
  };
};

// Verify compliance password
export const verifyCompliancePassword = async (password: string): Promise<VoiceAuthResult> => {
  await new Promise((resolve) => setTimeout(resolve, 500));

  const extractedPassword = extractPasswordFromVoice(password);

  if (extractedPassword === COMPLIANCE_CREDENTIALS.password) {
    return {
      success: true,
      message: 'Compliance login successful! Welcome to your dashboard.',
      user: {
        username: COMPLIANCE_CREDENTIALS.username,
        role: COMPLIANCE_CREDENTIALS.role,
        accessToken: 'compliance_token_' + Date.now(),
      },
    };
  }

  return {
    success: false,
    message: 'Incorrect password. Please try again.',
  };
};

// Verify vendor email
export const verifyVendorEmail = async (email: string): Promise<VoiceAuthResult> => {
  await new Promise((resolve) => setTimeout(resolve, 500));

  const extractedEmail = extractEmailFromVoice(email);
  const vendor = VENDOR_DATABASE.find((v) => v.email.toLowerCase() === extractedEmail.toLowerCase());

  if (vendor) {
    return {
      success: true,
      message: `Welcome back, ${vendor.name}! Logging you in to your vendor dashboard.`,
      user: {
        username: vendor.email,
        role: 'VENDOR',
        email: vendor.email,
        accessToken: 'vendor_token_' + Date.now(),
      },
    };
  }

  return {
    success: false,
    message: `Email "${extractedEmail}" not found in our vendor database. Please try another email.`,
  };
};

// Demo credentials for quick testing
export const getDemoCredentials = () => {
  return {
    admin: {
      username: ADMIN_CREDENTIALS.username,
      password: ADMIN_CREDENTIALS.password,
    },
    manager: {
      username: MANAGER_CREDENTIALS.username,
      password: MANAGER_CREDENTIALS.password,
    },
    compliance: {
      username: COMPLIANCE_CREDENTIALS.username,
      password: COMPLIANCE_CREDENTIALS.password,
    },
    vendors: VENDOR_DATABASE.map((v) => v.email),
  };
};
