package com.sequenceiq.freeipa.client;

public class FreeIpaClientBuildException extends FreeIpaClientException {

    public FreeIpaClientBuildException(String message) {
        super(message);
    }

    public FreeIpaClientBuildException(String message, int statusCode) {
        super(message, statusCode);
    }

    public FreeIpaClientBuildException(String message, Throwable cause) {
        super(message, cause);
    }
}
