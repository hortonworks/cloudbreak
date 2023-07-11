package com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterServicesRestartEventClass extends StackEvent {

    private boolean refreshNeeded;

    @JsonCreator
    public ClusterServicesRestartEventClass(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("refreshNeeded") boolean refreshNeeded) {
        super(selector, stackId);
        this.refreshNeeded = refreshNeeded;
    }

    public boolean isRefreshNeeded() {
        return refreshNeeded;
    }

    public ClusterServicesRestartEventClass setRefreshNeeded(boolean refreshNeeded) {
        this.refreshNeeded = refreshNeeded;
        return this;
    }
}
