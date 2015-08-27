package com.sequenceiq.cloudbreak.service.stack.connector;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;

public class OperationException extends RuntimeException {
    public OperationException(String message, CloudContext cloudContext, Throwable cause) {
        super(message + " [stack]: " + cloudContext, cause);
    }

    public OperationException(String message) {
        super(message);
    }
}
