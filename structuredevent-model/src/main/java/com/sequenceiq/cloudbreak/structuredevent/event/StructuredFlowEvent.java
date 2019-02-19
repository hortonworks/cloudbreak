package com.sequenceiq.cloudbreak.structuredevent.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StructuredFlowEvent extends StructuredEvent {
    private FlowDetails flow;

    private StackDetails stack;

    private ClusterDetails cluster;

    private ClusterDefinitionDetails clusterDefinitionDetails;

    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String exception;

    public StructuredFlowEvent() {
    }

    public StructuredFlowEvent(String type, OperationDetails operation, FlowDetails flow, StackDetails stack,
            ClusterDetails cluster, ClusterDefinitionDetails clusterDefinitionDetails) {
        super(type, operation);
        this.flow = flow;
        this.stack = stack;
        this.cluster = cluster;
        this.clusterDefinitionDetails = clusterDefinitionDetails;
    }

    public StructuredFlowEvent(OperationDetails operation, FlowDetails flow, StackDetails stack, ClusterDetails cluster,
            ClusterDefinitionDetails clusterDefinitionDetails) {
        this(StructuredFlowEvent.class.getSimpleName(), operation, flow, stack, cluster, clusterDefinitionDetails);
    }

    public StructuredFlowEvent(OperationDetails operation, FlowDetails flow, StackDetails stack,
            ClusterDetails cluster, ClusterDefinitionDetails clusterDefinitionDetails, String exception) {
        this(StructuredFlowEvent.class.getSimpleName(), operation, flow, stack, cluster, clusterDefinitionDetails);
        this.exception = exception;
    }

    @Override
    public String getStatus() {
        String state = flow.getFlowState();
        if ("unknown".equals(state)) {
            if (stack != null) {
                state = "STACK_" + stack.getDetailedStatus();
            }
            if (cluster != null) {
                state += " | CLUSTER_" + cluster.getStatus();
            }
        }
        return state;
    }

    @Override
    public Long getDuration() {
        return Math.max(0L, flow.getDuration());
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

    public ClusterDefinitionDetails getClusterDefinitionDetails() {
        return clusterDefinitionDetails;
    }

    public void setClusterDefinitionDetails(ClusterDefinitionDetails clusterDefinitionDetails) {
        this.clusterDefinitionDetails = clusterDefinitionDetails;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }
}
