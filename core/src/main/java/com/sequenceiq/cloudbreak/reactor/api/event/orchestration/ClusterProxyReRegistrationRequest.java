package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterProxyReRegistrationRequest extends ClusterPlatformRequest {
    private final String cloudPlatform;

    private final String finishedEvent;

    @JsonCreator
    public ClusterProxyReRegistrationRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("cloudPlatform") String cloudPlatform,
            @JsonProperty("finishedEvent") String finishedEvent) {
        super(stackId);
        this.cloudPlatform = cloudPlatform;
        this.finishedEvent = finishedEvent;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public String getFinishedEvent() {
        return finishedEvent;
    }
}
