package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Map;

public class ClusterManagerInitComponentsRequest extends AmbariComponentsRequest {

    public ClusterManagerInitComponentsRequest(Long stackId, String hostGroupName, String hostName, Map<String, String> components) {
        super(stackId, hostGroupName, hostName, components);
    }
}
