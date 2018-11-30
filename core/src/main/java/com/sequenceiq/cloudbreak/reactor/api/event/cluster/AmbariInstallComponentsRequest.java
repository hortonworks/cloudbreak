package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Map;

public class AmbariInstallComponentsRequest extends AmbariComponentsRequest {

    public AmbariInstallComponentsRequest(Long stackId, String hostGroupName, String hostName, Map<String, String> components) {
        super(stackId, hostGroupName, hostName, components);
    }
}
