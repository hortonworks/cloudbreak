package com.sequenceiq.cloudbreak.cm;

public class ClouderaManagerParcelActivationTimeoutException extends ClouderaManagerOperationFailedException {

    public ClouderaManagerParcelActivationTimeoutException(String message) {
        super(message);
    }

    public ClouderaManagerParcelActivationTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
