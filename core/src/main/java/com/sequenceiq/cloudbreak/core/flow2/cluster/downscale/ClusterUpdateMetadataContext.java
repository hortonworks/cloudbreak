package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import java.util.Set;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterScaleContext;
import com.sequenceiq.cloudbreak.domain.Stack;

class ClusterUpdateMetadataContext extends ClusterScaleContext {

    private final Set<String> hostNames;

    ClusterUpdateMetadataContext(String flowId, Stack stack, String hostGroupName, Set<String> hostNames) {
        super(flowId, stack, hostGroupName);
        this.hostNames = hostNames;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }
}
