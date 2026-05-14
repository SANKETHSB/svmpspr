import { detectRoleFromVoice } from './voiceAuth';

export interface CommandResult {
  type:
    | 'navigation'
    | 'admin_login'
    | 'manager_login'
    | 'compliance_login'
    | 'vendor_login'
    | 'form_action'
    | 'help'
    | 'unknown';
  action?: string;
  target?: string;
  message?: string;
}

// Navigation routes mapping
const NAVIGATION_ROUTES: Record<string, string> = {
  dashboard: '/dashboard',
  vendors: '/vendors',
  vendor: '/vendors',
  rfqs: '/rfqs',
  rfq: '/rfqs',
  purchase: '/purchase-orders',
  orders: '/purchase-orders',
  quotations: '/quotations',
  notifications: '/notifications',
  compliance: '/compliance',
  analytics: '/analytics',
  reports: '/reports',
  admin: '/admin',
  settings: '/settings',
};

// Check if transcript contains navigation command
const isNavigationCommand = (transcript: string): boolean => {
  const keywords = ['go to', 'open', 'navigate', 'show', 'take me to', 'go', 'view'];
  return keywords.some((keyword) => transcript.toLowerCase().includes(keyword));
};

// Extract destination from navigation command
const extractDestination = (transcript: string): string | null => {
  const lower = transcript.toLowerCase();

  for (const [keyword, route] of Object.entries(NAVIGATION_ROUTES)) {
    if (lower.includes(keyword)) {
      return route;
    }
  }

  return null;
};

// Check if transcript is a login command
const isLoginCommand = (transcript: string): boolean => {
  const lower = transcript.toLowerCase();
  return (
    lower.includes('i am') ||
    lower.includes('login') ||
    lower.includes('sign in') ||
    lower.includes('buddy')
  );
};

// Check if transcript is a form action
const isFormAction = (transcript: string): boolean => {
  const lower = transcript.toLowerCase();
  return (
    lower.includes('fill') ||
    lower.includes('enter') ||
    lower.includes('submit') ||
    lower.includes('send')
  );
};

// Check if transcript is a help request
const isHelpRequest = (transcript: string): boolean => {
  const lower = transcript.toLowerCase();
  return (
    lower.includes('help') ||
    lower.includes('what can you do') ||
    lower.includes('commands') ||
    lower.includes('assist')
  );
};

// Extract form field and value
interface FormExtraction {
  field: string;
  value: string;
}

const extractFormData = (transcript: string): FormExtraction | null => {
  const lower = transcript.toLowerCase();

  // Email pattern
  if (lower.includes('email')) {
    const emailRegex = /[\w\.-]+@[\w\.-]+\.\w+/;
    const match = transcript.match(emailRegex);
    if (match) {
      return { field: 'email', value: match[0] };
    }
  }

  // Password pattern (everything after "password" or similar)
  if (lower.includes('password')) {
    const parts = transcript.split(/password[:\s]*/i);
    if (parts.length > 1) {
      return { field: 'password', value: parts[1].trim() };
    }
  }

  // Phone pattern (10 digits)
  const phoneRegex = /(\d{3}[-.\s]?\d{3}[-.\s]?\d{4})/;
  const phoneMatch = transcript.match(phoneRegex);
  if (phoneMatch) {
    return { field: 'phone', value: phoneMatch[0] };
  }

  return null;
};

// Main command routing function
export const parseVoiceCommand = (transcript: string): CommandResult => {
  if (!transcript || transcript.trim().length === 0) {
    return {
      type: 'unknown',
      message: 'No speech detected. Please try again.',
    };
  }

  // Check for help request first
  if (isHelpRequest(transcript)) {
    return {
      type: 'help',
      message: 'Available commands: Say "I am admin buddy", "I am manager buddy", "I am compliance buddy", "vendor login", or "go to" followed by a page name.',
    };
  }

  // Check for login commands
  if (isLoginCommand(transcript)) {
    const role = detectRoleFromVoice(transcript);

    if (role === 'ADMIN') {
      return {
        type: 'admin_login',
        action: 'request_password',
        message: 'I heard admin. What is your password?',
      };
    }

    if (role === 'PROCUREMENT_MANAGER') {
      return {
        type: 'manager_login',
        action: 'request_password',
        message: 'I heard manager. What is your password?',
      };
    }

    if (role === 'COMPLIANCE_OFFICER') {
      return {
        type: 'compliance_login',
        action: 'request_password',
        message: 'I heard compliance. What is your password?',
      };
    }

    if (role === 'VENDOR') {
      return {
        type: 'vendor_login',
        action: 'request_email',
        message: 'Vendor login detected. Please provide your Gmail address.',
      };
    }
  }

  // Check for navigation commands
  if (isNavigationCommand(transcript)) {
    const destination = extractDestination(transcript);
    if (destination) {
      return {
        type: 'navigation',
        action: 'navigate',
        target: destination,
        message: `Navigating to ${destination}.`,
      };
    }
  }

  // Check for form actions
  if (isFormAction(transcript)) {
    const formData = extractFormData(transcript);
    if (formData) {
      return {
        type: 'form_action',
        action: 'fill_field',
        target: formData.field,
        message: formData.value,
      };
    }
  }

  // Unknown command
  return {
    type: 'unknown',
    message: 'Sorry, I did not understand that command. Please try again or say "help" for available commands.',
  };
};

// Get help message
export const getHelpMessage = (): string => {
  return `Here are some voice commands you can use:
    
    For login:
    - "I am admin buddy" (then provide password)
    - "I am manager buddy" (then provide password)
    - "I am compliance buddy" (then provide password)
    - "vendor login" (then provide Gmail)
    
    For navigation:
    - "go to dashboard"
    - "open vendors"
    - "navigate to rfqs"
    - "show purchase orders"
    - "take me to compliance"
    
    Other:
    - "help" for this message
    - "what can you do" for my capabilities`;
};
