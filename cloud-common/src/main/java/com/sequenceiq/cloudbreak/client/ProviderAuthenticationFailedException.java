package com.sequenceiq.cloudbreak.client;

import jakarta.ws.rs.ForbiddenException;

public class ProviderAuthenticationFailedException extends ForbiddenException {

    public ProviderAuthenticationFailedException(Throwable t) {
        super("Authentication to provider failed, check if your credential is valid or has enough permissions", t);
    }

    public ProviderAuthenticationFailedException(String msg) {
        super(msg);
    }

    public ProviderAuthenticationFailedException(String msg, Throwable t) {
        super(msg, t);
    }
}
