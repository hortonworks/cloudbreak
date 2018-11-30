package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class AmbariGatherInstalledComponentsRequest extends AbstractClusterScaleRequest {

    private final String hostName;

    public AmbariGatherInstalledComponentsRequest(Long stackId, String hostGroupName, String hostName) {
        super(stackId, hostGroupName);
        this.hostName = hostName;
    }

    public String getHostName() {
        return hostName;
    }
}
