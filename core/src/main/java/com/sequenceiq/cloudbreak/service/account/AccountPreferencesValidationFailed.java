package com.sequenceiq.cloudbreak.service.account;


public class AccountPreferencesValidationFailed extends Exception {

    public AccountPreferencesValidationFailed(String message) {
        super(message);
    }

    public AccountPreferencesValidationFailed(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountPreferencesValidationFailed(Throwable cause) {
        super(cause);
    }

}
