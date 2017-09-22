package com.sequenceiq.cloudbreak.structuredevent.event;

public class StructuredFlowErrorEvent extends StructuredFlowEvent {
    private String exception;

    public StructuredFlowErrorEvent(OperationDetails operation, FlowDetails flow, StackDetails stack, String exception) {
        this(operation, flow, stack, null, null, exception);
    }

    public StructuredFlowErrorEvent(OperationDetails operation, FlowDetails flow, StackDetails stack,
            ClusterDetails cluster, BlueprintDetails blueprint, String exception) {
        super(operation, flow, stack, cluster, blueprint);
        this.exception = exception;
    }

    public String getException() {
        return exception;
    }
}
