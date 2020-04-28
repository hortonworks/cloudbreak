package com.sequenceiq.freeipa.client.auth;

import com.sequenceiq.freeipa.client.FreeIpaClientException;

public class PasswordExpiredException extends FreeIpaClientException {
    public PasswordExpiredException() {
        super("Invalid password");
    }

    @Override
    public boolean isClientUnusable() {
        return true;
    }
}
