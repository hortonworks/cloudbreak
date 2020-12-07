package com.sequenceiq.freeipa.service.freeipa.dns;

public class DnsRecordConflictException extends RuntimeException {

    public DnsRecordConflictException(String message) {
        super(message);
    }
}
