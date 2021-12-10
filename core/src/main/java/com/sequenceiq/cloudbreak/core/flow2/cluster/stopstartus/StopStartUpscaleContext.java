package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public class StopStartUpscaleContext extends StackContext {

    private final String hostGroupName;

    private final Integer adjustment;

    private final Boolean singlePrimaryGateway;

    private final ClusterManagerType clusterManagerType;

    private final StackView stackView;

    public StopStartUpscaleContext(FlowParameters flowParameters, Stack stack, StackView stackView, CloudContext cloudContext, CloudCredential cloudCredentials,
            CloudStack cloudStack, String hostGroupName, Integer adjustment, Boolean singlePrimaryGateway,
            ClusterManagerType clusterManagerType) {
        super(flowParameters, stack, cloudContext, cloudCredentials, cloudStack);
        this.hostGroupName = hostGroupName;
        this.adjustment = adjustment;
        this.singlePrimaryGateway = singlePrimaryGateway;
        this.clusterManagerType = clusterManagerType;
        this.stackView = stackView;
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

    public ClusterManagerType getClusterManagerType() {
        return clusterManagerType;
    }

    public StackView getStackView() {
        return stackView;
    }
}
