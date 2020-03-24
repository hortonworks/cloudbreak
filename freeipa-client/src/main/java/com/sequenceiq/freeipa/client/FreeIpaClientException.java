package com.sequenceiq.freeipa.client;

import java.util.OptionalInt;

public class FreeIpaClientException extends Exception {

    private final OptionalInt statusCode;

    public FreeIpaClientException(String message) {
        super(message);
        statusCode = OptionalInt.empty();
    }

    public FreeIpaClientException(String message, int statusCode) {
        super(message);
        this.statusCode = OptionalInt.of(statusCode);
    }

    public FreeIpaClientException(String message, Throwable cause) {
        super(message, cause);
        statusCode = OptionalInt.empty();
    }

    public FreeIpaClientException(String message, Throwable cause, OptionalInt statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public OptionalInt getStatusCode() {
        return statusCode;
    }
}
