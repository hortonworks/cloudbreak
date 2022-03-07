package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Map;
import java.util.Set;

public class EnsureClusterComponentsAreStoppedRequest extends AmbariComponentsRequest {
    public EnsureClusterComponentsAreStoppedRequest(Long stackId, Set<String> hostGroups, String hostname, Map<String, String> components) {
        super(stackId, hostGroups, hostname, components);
    }
}
