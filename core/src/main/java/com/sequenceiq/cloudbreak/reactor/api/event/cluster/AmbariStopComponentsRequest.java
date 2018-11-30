package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Map;

public class AmbariStopComponentsRequest extends AmbariComponentsRequest {
    public AmbariStopComponentsRequest(Long stackId, String hostGroupName, String hostName, Map<String, String> components) {
        super(stackId, hostGroupName, hostName, components);
    }
}
