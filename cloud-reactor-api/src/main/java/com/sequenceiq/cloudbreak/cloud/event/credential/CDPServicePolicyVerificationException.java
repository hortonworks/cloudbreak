package com.sequenceiq.cloudbreak.cloud.event.credential;

public class CDPServicePolicyVerificationException extends RuntimeException {

    public CDPServicePolicyVerificationException(String message) {
        super(message);
    }

    public CDPServicePolicyVerificationException(String message, Exception errorDetails) {
        super(message, errorDetails);
    }
}
