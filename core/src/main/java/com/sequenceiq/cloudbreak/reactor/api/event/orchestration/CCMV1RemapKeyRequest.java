package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CCMV1RemapKeyRequest extends ClusterProxyReRegistrationRequest {
    private final String originalCrn;

    @JsonCreator
    public CCMV1RemapKeyRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("cloudPlatform") String cloudPlatform,
            @JsonProperty("finishedEvent") String finishedEvent,
            @JsonProperty("originalCrn") String originalCrn) {
        super(stackId, cloudPlatform, finishedEvent);
        this.originalCrn = originalCrn;
    }

    public String getOriginalCrn() {
        return originalCrn;
    }
}
