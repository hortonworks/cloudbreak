package com.sequenceiq.cloudbreak.service.consumption;

public class ConsumptionOperationFailedException extends RuntimeException {

    public ConsumptionOperationFailedException(String message) {
        super(message);
    }

    public ConsumptionOperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
