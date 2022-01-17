package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public class ClusterUpscaleContext extends ClusterViewContext {
    private final Map<String, Integer> hostGroupWithAdjustment;

    private final Boolean singlePrimaryGateway;

    private final String primaryGatewayHostName;

    private final ClusterManagerType clusterManagerType;

    private final Boolean repair;

    private final Boolean restartServices;

    public ClusterUpscaleContext(FlowParameters flowParameters, StackView stack, Map<String, Integer> hostGroupWithAdjustment, Boolean singlePrimaryGateway,
            String hostName, ClusterManagerType clusterManagerType, Boolean repair, Boolean restartServices) {
        super(flowParameters, stack);
        this.hostGroupWithAdjustment = hostGroupWithAdjustment;
        this.singlePrimaryGateway = singlePrimaryGateway;
        this.primaryGatewayHostName = hostName;
        this.clusterManagerType = clusterManagerType;
        this.repair = repair;
        this.restartServices = restartServices;
    }

    public Map<String, Integer> getHostGroupWithAdjustment() {
        return hostGroupWithAdjustment;
    }

    public Set<String> getHostGroups() {
        return hostGroupWithAdjustment.keySet();
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

    public Boolean isRepair() {
        return repair;
    }

    public Boolean isRestartServices() {
        return restartServices;
    }

}
