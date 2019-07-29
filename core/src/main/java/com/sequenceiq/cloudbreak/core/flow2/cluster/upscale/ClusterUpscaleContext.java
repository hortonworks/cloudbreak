package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public class ClusterUpscaleContext extends ClusterViewContext {
    private final String hostGroupName;

    private final Integer adjustment;

    private final Boolean singlePrimaryGateway;

    private final String primaryGatewayHostName;

    private final ClusterManagerType clusterManagerType;

    public ClusterUpscaleContext(FlowParameters flowParameters, StackView stack, String hostGroupName, Integer adjustment, Boolean singlePrimaryGateway,
            String hostName, ClusterManagerType clusterManagerType) {
        super(flowParameters, stack);
        this.hostGroupName = hostGroupName;
        this.adjustment = adjustment;
        this.singlePrimaryGateway = singlePrimaryGateway;
        primaryGatewayHostName = hostName;
        this.clusterManagerType = clusterManagerType;
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

    public ClusterManagerType getClusterManagerType() {
        return clusterManagerType;
    }
}
