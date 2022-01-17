package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UpscaleClusterRequest extends AbstractClusterScaleRequest {

    private boolean repair;

    private boolean restartServices;

    public UpscaleClusterRequest(Long stackId, Set<String> hostGroups, boolean repair, boolean restartServices) {
        super(stackId, hostGroups);
        this.repair = repair;
        this.restartServices = restartServices;
    }

    public boolean isRepair() {
        return repair;
    }

    public boolean isRestartServices() {
        return restartServices;
    }

}
