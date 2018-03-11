package com.sequenceiq.cloudbreak.structuredevent.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StructuredFlowEvent extends StructuredEvent {
    private FlowDetails flow;

    private StackDetails stack;

    private ClusterDetails cluster;

    private BlueprintDetails blueprint;

    public StructuredFlowEvent() {
    }

    public StructuredFlowEvent(OperationDetails operation, FlowDetails flow, StackDetails stack) {
        this(operation, flow, stack, null, null);
    }

    public StructuredFlowEvent(OperationDetails operation, FlowDetails flow, StackDetails stack, ClusterDetails cluster,
            BlueprintDetails blueprint) {
        this(StructuredFlowEvent.class.getSimpleName(), operation, flow, stack, cluster, blueprint);
    }

    public StructuredFlowEvent(String type, OperationDetails operation, FlowDetails flow, StackDetails stack,
            ClusterDetails cluster, BlueprintDetails blueprint) {
        super(type, operation);
        this.flow = flow;
        this.stack = stack;
        this.cluster = cluster;
        this.blueprint = blueprint;
    }

    public FlowDetails getFlow() {
        return flow;
    }

    public void setFlow(FlowDetails flow) {
        this.flow = flow;
    }

    public StackDetails getStack() {
        return stack;
    }

    public void setStack(StackDetails stack) {
        this.stack = stack;
    }

    public ClusterDetails getCluster() {
        return cluster;
    }

    public void setCluster(ClusterDetails cluster) {
        this.cluster = cluster;
    }

    public BlueprintDetails getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(BlueprintDetails blueprint) {
        this.blueprint = blueprint;
    }
}
