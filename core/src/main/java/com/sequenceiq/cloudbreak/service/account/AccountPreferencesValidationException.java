package com.sequenceiq.cloudbreak.service.account;


public class AccountPreferencesValidationException extends Exception {

    public AccountPreferencesValidationException(String message) {
        super(message);
    }

    public AccountPreferencesValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountPreferencesValidationException(Throwable cause) {
        super(cause);
    }

}
