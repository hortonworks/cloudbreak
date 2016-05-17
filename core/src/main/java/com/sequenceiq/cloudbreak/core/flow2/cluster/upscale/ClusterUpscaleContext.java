package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterScaleContext;
import com.sequenceiq.cloudbreak.domain.Stack;

public class ClusterUpscaleContext extends ClusterScaleContext {

    public ClusterUpscaleContext(String flowId, Stack stack, String hostGroupName) {
        super(flowId, stack, hostGroupName);
    }
}
