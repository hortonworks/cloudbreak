package com.sequenceiq.freeipa.client.auth;

import com.sequenceiq.freeipa.client.FreeIpaClientException;

public class PasswordExpiredException extends FreeIpaClientException {
    public PasswordExpiredException() {
        super("Invalid password");
    }

    public PasswordExpiredException(String message) {
        super(message);
    }

    public PasswordExpiredException(Throwable cause) {
        super(cause);
    }

    public PasswordExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public PasswordExpiredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
