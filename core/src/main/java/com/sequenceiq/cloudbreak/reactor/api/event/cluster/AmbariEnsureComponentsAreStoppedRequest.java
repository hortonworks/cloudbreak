package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Map;

public class AmbariEnsureComponentsAreStoppedRequest extends AmbariComponentsRequest {
    public AmbariEnsureComponentsAreStoppedRequest(Long stackId, String hostGroupName, String hostname, Map<String, String> components) {
        super(stackId, hostGroupName, hostname, components);
    }
}
