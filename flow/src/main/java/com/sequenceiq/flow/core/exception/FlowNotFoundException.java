package com.sequenceiq.flow.core.exception;

public class FlowNotFoundException extends RuntimeException {

    public FlowNotFoundException(String flowId) {
        super("Flow not found with id: " + flowId);
    }

}
