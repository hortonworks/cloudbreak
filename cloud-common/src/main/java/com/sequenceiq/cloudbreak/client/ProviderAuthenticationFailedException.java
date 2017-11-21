package com.sequenceiq.cloudbreak.client;

import org.springframework.security.access.AccessDeniedException;

public class ProviderAuthenticationFailedException extends AccessDeniedException {

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
