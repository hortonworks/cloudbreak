package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Stack;

public class ClusterUpscaleHostContext extends ClusterUpscaleContext {

    private final HostGroup hostGroup;
    private final Set<HostMetadata> hostMetadata;

    ClusterUpscaleHostContext(String flowId, Stack stack, HostGroup hostGroup, Set<HostMetadata> hostMetadata) {
        super(flowId, stack);
        this.hostGroup = hostGroup;
        this.hostMetadata = hostMetadata;
    }

    public HostGroup getHostGroup() {
        return hostGroup;
    }

    public Set<HostMetadata> getHostMetadata() {
        return hostMetadata;
    }
}
