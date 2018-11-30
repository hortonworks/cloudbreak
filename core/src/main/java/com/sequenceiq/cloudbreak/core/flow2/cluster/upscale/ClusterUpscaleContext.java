package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;

public class ClusterUpscaleContext extends ClusterViewContext {
    private final String hostGroupName;

    private final Integer adjustment;

    private final Boolean singlePrimaryGateway;

    private final String primaryGatewayHostName;

    public ClusterUpscaleContext(String flowId, StackView stack, String hostGroupName, Integer adjustment, Boolean singlePrimaryGateway, String hostName) {
        super(flowId, stack);
        this.hostGroupName = hostGroupName;
        this.adjustment = adjustment;
        this.singlePrimaryGateway = singlePrimaryGateway;
        this.primaryGatewayHostName = hostName;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public Integer getAdjustment() {
        return adjustment;
    }

    public Boolean isSinglePrimaryGateway() {
        return singlePrimaryGateway;
    }

    public String getPrimaryGatewayHostName() {
        return primaryGatewayHostName;
    }
}
