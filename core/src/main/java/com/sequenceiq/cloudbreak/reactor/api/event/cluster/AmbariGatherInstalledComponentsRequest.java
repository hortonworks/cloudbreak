package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class AmbariGatherInstalledComponentsRequest extends AbstractClusterScaleRequest {

    private final String hostName;

    public AmbariGatherInstalledComponentsRequest(Long stackId, Set<String> hostGroupNames, String hostName) {
        super(stackId, hostGroupNames);
        this.hostName = hostName;
    }

    public String getHostName() {
        return hostName;
    }
}