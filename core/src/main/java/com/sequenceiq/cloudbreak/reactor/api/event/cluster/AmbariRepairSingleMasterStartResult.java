package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class AmbariRepairSingleMasterStartResult extends AbstractClusterScaleRequest {
    public AmbariRepairSingleMasterStartResult(Long stackId, Set<String> hostGroups) {
        super(stackId, hostGroups);
    }
}