package com.sequenceiq.cloudbreak.certificate;

public class PkiException extends RuntimeException {

    public PkiException(String message, Throwable cause) {
        super(message, cause);
    }

    public PkiException(String message) {
        super(message);
    }
}


