package com.sequenceiq.cloudbreak.core.flow2.exception;

public class FlowNotFoundException extends RuntimeException {

    public FlowNotFoundException(String flowId) {
        super("Flow not found with id: " + flowId);
    }

}
