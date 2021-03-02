package com.sequenceiq.cloudbreak.structuredevent.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StructuredSyncEvent extends StructuredEvent {

    private StackDetails stack;

    private ClusterDetails cluster;

    private BlueprintDetails blueprintDetails;

    public StructuredSyncEvent() {
        super(StructuredSyncEvent.class.getSimpleName());
    }

    public StructuredSyncEvent(OperationDetails operation, StackDetails stack,
            ClusterDetails cluster, BlueprintDetails blueprintDetails) {
        super(StructuredSyncEvent.class.getSimpleName(), operation);
        this.stack = stack;
        this.cluster = cluster;
        this.blueprintDetails = blueprintDetails;
    }

    @Override
    public String getStatus() {
        return SENT;
    }

    @Override
    public Long getDuration() {
        return ZERO;
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

    public BlueprintDetails getBlueprintDetails() {
        return blueprintDetails;
    }

    public void setBlueprintDetails(BlueprintDetails blueprintDetails) {
        this.blueprintDetails = blueprintDetails;
    }
}
