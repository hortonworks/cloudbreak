package com.sequenceiq.cloudbreak.core.flow;

public class CloudbreakFlowException extends RuntimeException {

    public CloudbreakFlowException(String message) {
        super(message);
    }

    public CloudbreakFlowException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudbreakFlowException(Throwable cause) {
        super(cause);
    }

}
