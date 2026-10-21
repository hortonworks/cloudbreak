package com.sequenceiq.cloudbreak.cloud.exception;

import java.util.List;

public class InsufficientCapacityException extends CloudConnectorException {

    public InsufficientCapacityException(String message) {
        super(message);
    }

    public InsufficientCapacityException(String message, Throwable cause) {
        super(message, cause);
    }

    public InsufficientCapacityException(Throwable cause) {
        super(cause);
    }

    protected InsufficientCapacityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Creates the exception thrown when every instance type candidate of a resource failed due to capacity shortage, with the same message
     * on every cloud provider, whether the capacity failures were real or simulated.
     */
    public static InsufficientCapacityException allInstanceTypesExhausted(String operationDescription, List<String> instanceTypes, String reason,
            Throwable cause) {
        return new InsufficientCapacityException(String.format("%s: all instance types %s exhausted due to capacity shortage. Reason: %s",
                operationDescription, instanceTypes, reason), cause);
    }
}
