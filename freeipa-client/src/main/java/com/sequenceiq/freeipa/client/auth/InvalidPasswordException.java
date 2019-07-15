package com.sequenceiq.freeipa.client.auth;

import com.sequenceiq.freeipa.client.FreeIpaClientException;

public class InvalidPasswordException extends FreeIpaClientException {
    public InvalidPasswordException() {
        super("Invalid password");
    }

    public InvalidPasswordException(String message) {
        super(message);
    }

    public InvalidPasswordException(Throwable cause) {
        super(cause);
    }

    public InvalidPasswordException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPasswordException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
