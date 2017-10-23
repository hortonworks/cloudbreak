package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.domain.StackView;

public class ClusterUpscaleContext extends ClusterViewContext {
    private final String hostGroupName;

    private final Integer adjustment;

    public ClusterUpscaleContext(String flowId, StackView stack, String hostGroupName, Integer adjustment) {
        super(flowId, stack);
        this.hostGroupName = hostGroupName;
        this.adjustment = adjustment;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public Integer getAdjustment() {
        return adjustment;
    }
}
