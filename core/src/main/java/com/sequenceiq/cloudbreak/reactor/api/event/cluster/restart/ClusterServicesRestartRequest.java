package com.sequenceiq.cloudbreak.reactor.api.event.cluster.restart;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterServicesRestartRequest extends ClusterPlatformRequest {

    private final boolean rollingRestart;

    private final boolean restartStaleServices;

    private final boolean reallocateMemory;

    @JsonCreator
    public ClusterServicesRestartRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("rollingRestart") boolean rollingRestart,
            @JsonProperty("restartStaleServices") boolean restartStaleServices,
            @JsonProperty("reallocateMemory") boolean reallocateMemory) {
        super(stackId);
        this.rollingRestart = rollingRestart;
        this.restartStaleServices = restartStaleServices;
        this.reallocateMemory = reallocateMemory;
    }

    public boolean isRollingRestart() {
        return rollingRestart;
    }

    public boolean isRestartStaleServices() {
        return restartStaleServices;
    }

    public boolean isReallocateMemory() {
        return reallocateMemory;
    }
}
