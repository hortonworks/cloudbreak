package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public class StopStartDownscaleContext extends StackContext {

    private final String hostGroupName;

    private final Set<Long> hostIdsToRemove;

    private final Boolean singlePrimaryGateway;

    private final ClusterManagerType clusterManagerType;

    private final StackView stackView;

    public StopStartDownscaleContext(FlowParameters flowParameters, Stack stack, StackView stackView,
            CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack,
            String hostGroupName, Set<Long> hostIdsToRemove,
            Boolean singlePrimaryGateway,
            ClusterManagerType clusterManagerType) {
        super(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
        this.hostGroupName = hostGroupName;
        this.hostIdsToRemove = hostIdsToRemove;
        this.singlePrimaryGateway = singlePrimaryGateway;
        this.clusterManagerType = clusterManagerType;
        this.stackView = stackView;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public Set<Long> getHostIdsToRemove() {
        return hostIdsToRemove;
    }

    // TODO CB-14929: Is this ever used? / is this needed in mutli-CM instances?
    public Boolean getSinglePrimaryGateway() {
        return singlePrimaryGateway;
    }

    public ClusterManagerType getClusterManagerType() {
        return clusterManagerType;
    }

    public StackView getStackView() {
        return stackView;
    }

    @Override
    public String toString() {
        return "StopStartDownscaleContext{" +
                "hostGroupName='" + hostGroupName + '\'' +
                ", hostIdsToRemove=" + hostIdsToRemove +
                ", singlePrimaryGateway=" + singlePrimaryGateway +
                ", clusterManagerType=" + clusterManagerType +
                ", stackView=" + stackView +
                '}';
    }
}
