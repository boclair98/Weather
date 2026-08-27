package com.example.WebSideProject.service;

/**
 * Raised when a signed-in owner requests verification codes too quickly.
 * Keeping this as a dedicated exception lets the API return a useful 429 and
 * Retry-After value instead of treating a normal anti-abuse response as a
 * malformed request.
 */
public class EmailVerificationCooldownException extends IllegalArgumentException {

    private final long retryAfterSeconds;

    public EmailVerificationCooldownException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
