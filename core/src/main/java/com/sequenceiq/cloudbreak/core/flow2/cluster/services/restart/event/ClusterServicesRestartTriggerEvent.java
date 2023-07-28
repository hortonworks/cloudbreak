package com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterServicesRestartTriggerEvent extends StackEvent {

    private boolean refreshNeeded;

    @JsonCreator
    public ClusterServicesRestartTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("refreshNeeded") boolean refreshNeeded) {
        super(selector, stackId);
        this.refreshNeeded = refreshNeeded;
    }

    public boolean isRefreshNeeded() {
        return refreshNeeded;
    }

    public void setRefreshNeeded(boolean refreshNeeded) {
        this.refreshNeeded = refreshNeeded;
    }
}
