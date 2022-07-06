package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterProxyGatewayRegistrationRequest extends StackEvent {

    private final String cloudPlatform;

    @JsonCreator
    public ClusterProxyGatewayRegistrationRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("cloudPlatform") String cloudPlatform) {
        super(stackId);
        this.cloudPlatform = cloudPlatform;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }
}
