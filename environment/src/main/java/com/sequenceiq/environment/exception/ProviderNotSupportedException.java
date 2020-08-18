package com.sequenceiq.environment.exception;

public class ProviderNotSupportedException extends RuntimeException {

    private static final ProviderNotSupportedException OUR_INSTANCE = new ProviderNotSupportedException();

    private ProviderNotSupportedException() {
    }

    public static ProviderNotSupportedException getInstance() {
        return OUR_INSTANCE;
    }
}
