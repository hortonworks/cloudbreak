package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Map;
import java.util.Set;

public class ClusterManagerInitComponentsRequest extends AmbariComponentsRequest {

    public ClusterManagerInitComponentsRequest(Long stackId, Set<String> hostGroups, String hostName, Map<String, String> components) {
        super(stackId, hostGroups, hostName, components);
    }
}
