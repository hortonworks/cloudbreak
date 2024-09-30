package com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterServicesRestartTriggerEvent extends StackEvent {

    private final boolean refreshNeeded;

    private final boolean rollingRestart;

    public ClusterServicesRestartTriggerEvent(String selector, Long stackId, boolean refreshNeeded, boolean rollingRestart) {
        super(selector, stackId);
        this.refreshNeeded = refreshNeeded;
        this.rollingRestart = rollingRestart;
    }

    @JsonCreator
    public ClusterServicesRestartTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("refreshNeeded") boolean refreshNeeded,
            @JsonProperty("rollingRestart") boolean rollingRestart,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.refreshNeeded = refreshNeeded;
        this.rollingRestart = rollingRestart;
    }

    public boolean isRefreshNeeded() {
        return refreshNeeded;
    }

    public boolean isRollingRestart() {
        return rollingRestart;
    }
}
