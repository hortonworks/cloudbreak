package com.sequenceiq.cloudbreak.ccm.exception;

public class CcmV2Exception extends RuntimeException {

    public CcmV2Exception(String message) {
        super(message);
    }

    public CcmV2Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
