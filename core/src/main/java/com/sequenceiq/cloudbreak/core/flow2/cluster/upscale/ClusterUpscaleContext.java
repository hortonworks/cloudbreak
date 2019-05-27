package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public class ClusterUpscaleContext extends ClusterViewContext {
    private final String hostGroupName;

    private final Integer adjustment;

    private final Boolean singlePrimaryGateway;

    private final String primaryGatewayHostName;

    public ClusterUpscaleContext(FlowParameters flowParameters, StackView stack, String hostGroupName, Integer adjustment, Boolean singlePrimaryGateway,
            String hostName) {
        super(flowParameters, stack);
        this.hostGroupName = hostGroupName;
        this.adjustment = adjustment;
        this.singlePrimaryGateway = singlePrimaryGateway;
        primaryGatewayHostName = hostName;
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
