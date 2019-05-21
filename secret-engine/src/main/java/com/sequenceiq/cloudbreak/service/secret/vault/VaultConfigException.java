package com.sequenceiq.cloudbreak.service.secret.vault;

public class VaultConfigException extends RuntimeException {

    public VaultConfigException() {
    }

    public VaultConfigException(String message) {
        super(message);
    }

    public VaultConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public VaultConfigException(Throwable cause) {
        super(cause);
    }

    public VaultConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
