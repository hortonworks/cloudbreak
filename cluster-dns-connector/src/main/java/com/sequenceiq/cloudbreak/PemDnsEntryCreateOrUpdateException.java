package com.sequenceiq.cloudbreak;

public class PemDnsEntryCreateOrUpdateException extends Exception {

    public PemDnsEntryCreateOrUpdateException(String message) {
        super(message);
    }

    public PemDnsEntryCreateOrUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    public PemDnsEntryCreateOrUpdateException(Throwable cause) {
        super(cause);
    }
}
