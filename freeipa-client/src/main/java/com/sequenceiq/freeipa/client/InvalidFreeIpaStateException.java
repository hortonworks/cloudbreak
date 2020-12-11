package com.sequenceiq.freeipa.client;

public class InvalidFreeIpaStateException extends RuntimeException {
    public InvalidFreeIpaStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
