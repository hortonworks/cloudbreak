package com.sequenceiq.cloudbreak.structuredevent.event;

public class StructuredFlowEvent extends StructuredEvent {
    private FlowDetails flow;

    private StackDetails stack;

    private ClusterDetails cluster;

    private BlueprintDetails blueprint;

    public StructuredFlowEvent(OperationDetails operation, FlowDetails flow, StackDetails stack) {
        this(operation, flow, stack, null, null);
    }

    public StructuredFlowEvent(OperationDetails operation, FlowDetails flow, StackDetails stack, ClusterDetails cluster,
            BlueprintDetails blueprint) {
        super(StructuredFlowEvent.class.getSimpleName(), operation);
        this.flow = flow;
        this.stack = stack;
        this.cluster = cluster;
        this.blueprint = blueprint;
    }

    public FlowDetails getFlow() {
        return flow;
    }

    public StackDetails getStack() {
        return stack;
    }

    public ClusterDetails getCluster() {
        return cluster;
    }

    public BlueprintDetails getBlueprint() {
        return blueprint;
    }
}
