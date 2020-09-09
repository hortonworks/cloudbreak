package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UpscaleClusterRequest extends AbstractClusterScaleRequest {

    private boolean repair;

    private boolean restartServices;

    public UpscaleClusterRequest(Long stackId, String hostGroupName, boolean repair, boolean restartServices) {
        super(stackId, hostGroupName);
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
