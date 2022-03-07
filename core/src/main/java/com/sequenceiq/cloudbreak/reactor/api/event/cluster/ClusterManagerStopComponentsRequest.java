package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Map;
import java.util.Set;

public class ClusterManagerStopComponentsRequest extends AmbariComponentsRequest {
    public ClusterManagerStopComponentsRequest(Long stackId, Set<String> hostGroups, String hostName, Map<String, String> components) {
        super(stackId, hostGroups, hostName, components);
    }
}
