package com.sequenceiq.freeipa.client;

public class FreeIpaHostNotAvailableException extends Exception {

    public FreeIpaHostNotAvailableException(String message) {
        super(message);
    }

    public FreeIpaHostNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
