package com.sequenceiq.freeipa.client;

public class FreeIpaClientExceptionWrapper extends RuntimeException {
    public FreeIpaClientExceptionWrapper(FreeIpaClientException e) {
        super(e);
    }

    public FreeIpaClientException getWrappedException() {
        return (FreeIpaClientException) getCause();
    }
}
