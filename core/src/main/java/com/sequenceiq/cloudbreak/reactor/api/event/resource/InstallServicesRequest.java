package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class InstallServicesRequest extends AbstractClusterScaleRequest {

    public InstallServicesRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
