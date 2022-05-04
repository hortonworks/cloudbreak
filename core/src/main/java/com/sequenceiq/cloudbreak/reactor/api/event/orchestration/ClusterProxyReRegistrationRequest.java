package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterProxyReRegistrationRequest extends ClusterPlatformRequest {
    private String cloudPlatform;

    private String finishedEvent;

    public ClusterProxyReRegistrationRequest(Long stackId, String cloudPlatform, String finishedEvent) {
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
