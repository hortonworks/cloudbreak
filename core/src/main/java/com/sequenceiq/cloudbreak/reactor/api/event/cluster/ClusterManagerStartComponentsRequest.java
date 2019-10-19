package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Map;

public class ClusterManagerStartComponentsRequest extends AmbariComponentsRequest {

    public ClusterManagerStartComponentsRequest(Long stackId, String hostGroupName, String hostName, Map<String, String> components) {
        super(stackId, hostGroupName, hostName, components);
    }
}
