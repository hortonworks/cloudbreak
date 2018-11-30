package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

    public class AmbariRepairSingleMasterStartResult extends AbstractClusterScaleRequest {
    public AmbariRepairSingleMasterStartResult(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
