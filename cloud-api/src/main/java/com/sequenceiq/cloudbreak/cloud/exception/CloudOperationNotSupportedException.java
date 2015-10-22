package com.sequenceiq.cloudbreak.cloud.exception;

/**
 * This {@link RuntimeException} is thrown in case an operation is not supported on a Cloud Platfrom.
 */
public class CloudOperationNotSupportedException extends CloudConnectorException {

    public CloudOperationNotSupportedException(String message) {
        super(message);
    }

    public CloudOperationNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudOperationNotSupportedException(Throwable cause) {
        super(cause);
    }

}