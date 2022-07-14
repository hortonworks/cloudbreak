package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.flow.core.FlowParameters;

public class StopStartDownscaleContext extends StackContext {

    private final String hostGroupName;

    private final Set<Long> hostIdsToRemove;

    private final ClusterManagerType clusterManagerType;

    public StopStartDownscaleContext(FlowParameters flowParameters, StackDtoDelegate stack,
            CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack,
            String hostGroupName, Set<Long> hostIdsToRemove,
            ClusterManagerType clusterManagerType) {
        super(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
        this.hostGroupName = hostGroupName;
        this.hostIdsToRemove = hostIdsToRemove;
        this.clusterManagerType = clusterManagerType;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public Set<Long> getHostIdsToRemove() {
        return hostIdsToRemove;
    }

    public ClusterManagerType getClusterManagerType() {
        return clusterManagerType;
    }

    @Override
    public String toString() {
        return "StopStartDownscaleContext{" +
                "hostGroupName='" + hostGroupName + '\'' +
                ", hostIdsToRemove=" + hostIdsToRemove +
                ", clusterManagerType=" + clusterManagerType +
                '}';
    }
}
