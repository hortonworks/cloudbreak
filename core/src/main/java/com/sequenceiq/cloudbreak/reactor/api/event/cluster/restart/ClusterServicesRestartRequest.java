package com.sequenceiq.cloudbreak.reactor.api.event.cluster.restart;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterServicesRestartRequest extends ClusterPlatformRequest {

    private final boolean rollingRestart;

    @JsonCreator
    public ClusterServicesRestartRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("rollingRestart") boolean rollingRestart) {
        super(stackId);
        this.rollingRestart = rollingRestart;
    }

    public boolean isRollingRestart() {
        return rollingRestart;
    }
}
