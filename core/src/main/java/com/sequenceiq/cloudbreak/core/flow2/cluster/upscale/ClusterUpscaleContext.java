package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterMinimalContext;
import com.sequenceiq.cloudbreak.domain.StackMinimal;

public class ClusterUpscaleContext extends ClusterMinimalContext {
    private final String hostGroupName;

    private final Integer adjustment;

    public ClusterUpscaleContext(String flowId, StackMinimal stack, String hostGroupName, Integer adjustment) {
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
