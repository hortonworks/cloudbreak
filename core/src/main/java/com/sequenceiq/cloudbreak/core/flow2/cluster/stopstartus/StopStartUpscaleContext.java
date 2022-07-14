package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.flow.core.FlowParameters;

public class StopStartUpscaleContext extends StackContext {

    private final String hostGroupName;

    private final Integer adjustment;

    private final ClusterManagerType clusterManagerType;

    public StopStartUpscaleContext(FlowParameters flowParameters, StackDtoDelegate stack, CloudContext cloudContext, CloudCredential cloudCredentials,
            CloudStack cloudStack, String hostGroupName, Integer adjustment, ClusterManagerType clusterManagerType) {
        super(flowParameters, stack, cloudContext, cloudCredentials, cloudStack);
        this.hostGroupName = hostGroupName;
        this.adjustment = adjustment;
        this.clusterManagerType = clusterManagerType;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public Integer getAdjustment() {
        return adjustment;
    }

    public ClusterManagerType getClusterManagerType() {
        return clusterManagerType;
    }
}
