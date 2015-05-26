package com.sequenceiq.cloudbreak.core.flow;

public class FlowCancelledException extends RuntimeException {
    public FlowCancelledException(String message) {
        super(message);
    }
}
