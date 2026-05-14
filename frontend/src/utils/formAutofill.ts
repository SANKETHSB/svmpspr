// Find form input by type or name
export const findFormInput = (
  fieldName: string
): HTMLInputElement | HTMLTextAreaElement | null => {
  const fieldLower = fieldName.toLowerCase();

  // Try to find by type
  const byType = document.querySelector<HTMLInputElement>(
    `input[type="${fieldLower}"], input[name*="${fieldLower}"]`
  );
  if (byType) return byType;

  // Try to find by name
  const byName = document.querySelector<HTMLInputElement>(
    `input[name*="${fieldLower}"], textarea[name*="${fieldLower}"]`
  );
  if (byName) return byName;

  // Try to find by placeholder
  const byPlaceholder = document.querySelector<HTMLInputElement>(
    `input[placeholder*="${fieldLower}" i], textarea[placeholder*="${fieldLower}" i]`
  );
  if (byPlaceholder) return byPlaceholder;

  // Try to find by aria-label
  const byLabel = document.querySelector<HTMLInputElement>(
    `input[aria-label*="${fieldLower}" i], textarea[aria-label*="${fieldLower}" i]`
  );
  if (byLabel) return byLabel;

  return null;
};

// Auto-fill form field
export const autoFillField = (fieldName: string, value: string): boolean => {
  const input = findFormInput(fieldName);

  if (!input) {
    console.warn(`[Voice] Could not find form field: ${fieldName}`);
    return false;
  }

  // Set the value
  input.value = value;

  // Trigger change event
  const event = new Event('change', { bubbles: true });
  input.dispatchEvent(event);

  // Also trigger input event for React components
  const inputEvent = new Event('input', { bubbles: true });
  input.dispatchEvent(inputEvent);

  // Focus and blur to trigger validation
  input.focus();
  input.blur();

  console.log(`[Voice] Auto-filled ${fieldName} with value`);
  return true;
};

// Submit current form
export const submitForm = (): boolean => {
  // Try to find form submit button
  const submitButton = document.querySelector<HTMLButtonElement>(
    'button[type="submit"], button:contains("Submit"), button:contains("Login"), button:contains("Sign In")'
  );

  if (submitButton) {
    submitButton.click();
    console.log('[Voice] Form submitted');
    return true;
  }

  // Try to find and submit form element directly
  const form = document.querySelector<HTMLFormElement>('form');
  if (form) {
    form.submit();
    console.log('[Voice] Form submitted');
    return true;
  }

  console.warn('[Voice] Could not find form or submit button');
  return false;
};

// Fill multiple fields at once
export const autoFillFields = (
  fields: Record<string, string>
): { success: boolean; filledFields: string[] } => {
  const filledFields: string[] = [];

  for (const [fieldName, value] of Object.entries(fields)) {
    if (autoFillField(fieldName, value)) {
      filledFields.push(fieldName);
    }
  }

  return {
    success: filledFields.length === Object.keys(fields).length,
    filledFields,
  };
};

// Clear form fields
export const clearFormFields = (): void => {
  const inputs = document.querySelectorAll<
    HTMLInputElement | HTMLTextAreaElement
  >('input[type="text"], input[type="email"], input[type="password"], textarea');

  inputs.forEach((input) => {
    input.value = '';
    input.dispatchEvent(new Event('change', { bubbles: true }));
    input.dispatchEvent(new Event('input', { bubbles: true }));
  });

  console.log('[Voice] Cleared all form fields');
};

// Check if form is currently visible
export const isFormVisible = (): boolean => {
  const form = document.querySelector<HTMLFormElement>('form');
  if (!form) return false;

  const rect = form.getBoundingClientRect();
  return rect.top >= 0 && rect.left >= 0 && rect.bottom <= window.innerHeight && rect.right <= window.innerWidth;
};

// Get all form fields
export const getFormFields = (): Array<{
  name: string;
  type: string;
  value: string;
}> => {
  const fields: Array<{ name: string; type: string; value: string }> = [];
  const inputs = document.querySelectorAll<
    HTMLInputElement | HTMLTextAreaElement
  >('input, textarea');

  inputs.forEach((input) => {
    fields.push({
      name: input.name || input.id || 'unknown',
      type: input instanceof HTMLInputElement ? input.type : 'textarea',
      value: input.value,
    });
  });

  return fields;
};
