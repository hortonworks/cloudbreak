package com.sequenceiq.freeipa.client.auth;

import com.sequenceiq.freeipa.client.FreeIpaClientException;

public class InvalidPasswordException extends FreeIpaClientException {
    public InvalidPasswordException() {
        super("Invalid password");
    }

    @Override
    public boolean isClientUnusable() {
        return true;
    }
}
