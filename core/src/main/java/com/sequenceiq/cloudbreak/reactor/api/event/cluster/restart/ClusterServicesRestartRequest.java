package com.sequenceiq.cloudbreak.reactor.api.event.cluster.restart;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterServicesRestartRequest extends ClusterPlatformRequest {

    private final boolean rollingRestart;

    private final boolean restartStaleServices;

    @JsonCreator
    public ClusterServicesRestartRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("rollingRestart") boolean rollingRestart,
            @JsonProperty("restartStaleServices") boolean restartStaleServices) {
        super(stackId);
        this.rollingRestart = rollingRestart;
        this.restartStaleServices = restartStaleServices;
    }

    public boolean isRollingRestart() {
        return rollingRestart;
    }

    public boolean isRestartStaleServices() {
        return restartStaleServices;
    }
}
