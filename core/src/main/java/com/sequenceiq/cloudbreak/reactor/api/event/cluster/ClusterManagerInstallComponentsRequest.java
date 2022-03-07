package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Map;
import java.util.Set;

public class ClusterManagerInstallComponentsRequest extends AmbariComponentsRequest {

    public ClusterManagerInstallComponentsRequest(Long stackId, Set<String> hostGroups, String hostName, Map<String, String> components) {
        super(stackId, hostGroups, hostName, components);
    }
}
