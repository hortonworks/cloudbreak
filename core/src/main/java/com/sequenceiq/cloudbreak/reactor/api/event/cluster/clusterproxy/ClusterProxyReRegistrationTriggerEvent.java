package com.sequenceiq.cloudbreak.reactor.api.event.cluster.clusterproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterProxyReRegistrationTriggerEvent extends StackEvent {
    private final String originalCrn;

    @JsonCreator
    public ClusterProxyReRegistrationTriggerEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("originalCrn") String originalCrn) {
        super(selector, stackId);
        this.originalCrn = originalCrn;
    }

    public String getOriginalCrn() {
        return originalCrn;
    }
}
