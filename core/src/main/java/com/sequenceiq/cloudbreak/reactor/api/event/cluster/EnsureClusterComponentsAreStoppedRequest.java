package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Map;

public class EnsureClusterComponentsAreStoppedRequest extends AmbariComponentsRequest {
    public EnsureClusterComponentsAreStoppedRequest(Long stackId, String hostGroupName, String hostname, Map<String, String> components) {
        super(stackId, hostGroupName, hostname, components);
    }
}
