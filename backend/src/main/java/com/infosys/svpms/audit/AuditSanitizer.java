package com.infosys.svpms.audit;

import java.util.regex.Pattern;

/**
 * Masks sensitive fields in audit oldValue / newValue payloads (US 14 #10).
 *
 * Inputs are typically JSON-ish strings produced by service layers
 * (e.g., {@code "{password=secret123, email=foo@bar}"}). This class
 * applies regex-based masking for known sensitive keys so that the
 * audit table never persists secrets in plaintext.
 *
 * Recognized sensitive keys (case-insensitive):
 *   password, passwd, pwd, secret, token, accessToken, refreshToken,
 *   apiKey, api_key, cvv, cardNumber, card_number, pan, otp, jwt,
 *   authorization, sessionId
 */
public final class AuditSanitizer {

    private AuditSanitizer() {}

    private static final String MASK = "***REDACTED***";

    /** Matches both JSON-style ("key":"val") and toString-style (key=val) occurrences. */
    private static final Pattern SENSITIVE = Pattern.compile(
        "(?i)(\"?(password|passwd|pwd|secret|token|accessToken|refreshToken|apiKey|api_key|" +
        "cvv|cardNumber|card_number|pan|otp|jwt|authorization|sessionId)\"?\\s*[:=]\\s*)" +
        "(\"[^\"]*\"|'[^']*'|[^,}\\s]+)"
    );

    /**
     * Returns a sanitized copy of {@code value} with sensitive substrings replaced.
     * Returns {@code null} if input is null. Returns the input unchanged if no
     * sensitive markers are detected (caller can check {@link #containsSensitive}).
     */
    public static String mask(String value) {
        if (value == null || value.isEmpty()) return value;
        return SENSITIVE.matcher(value).replaceAll("$1\"" + MASK + "\"");
    }

    /** Returns {@code true} if the value contains a sensitive key marker. */
    public static boolean containsSensitive(String value) {
        return value != null && SENSITIVE.matcher(value).find();
    }
}
