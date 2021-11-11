package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.flow.core.FlowParameters;

public class StackScalingFlowContext extends StackContext {
    private final Set<String> instanceIds;

    private final String instanceGroupName;

    private final Integer adjustment;

    private final Set<String> hostNames;

    private final boolean repair;

    private final NetworkScaleDetails networkScaleDetails;

    public StackScalingFlowContext(FlowParameters flowParameters, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential,
            CloudStack cloudStack, String instanceGroupName, Set<String> instanceIds, Integer adjustment, boolean repair) {
        this(flowParameters, stack, cloudContext, cloudCredential, cloudStack, instanceGroupName, instanceIds, adjustment, Collections.emptySet(), repair,
                NetworkScaleDetails.getEmpty());
    }

    public StackScalingFlowContext(FlowParameters flowParameters, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential,
            CloudStack cloudStack, String instanceGroupName, Set<String> instanceIds, Integer adjustment, Set<String> hostNames, boolean repair,
            NetworkScaleDetails networkScaleDetails) {
        super(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
        this.instanceGroupName = instanceGroupName;
        this.instanceIds = instanceIds;
        this.adjustment = adjustment;
        this.hostNames = hostNames;
        this.repair = repair;
        this.networkScaleDetails = networkScaleDetails;
    }

    public Collection<String> getInstanceIds() {
        return instanceIds;
    }

    public String getInstanceGroupName() {
        return instanceGroupName;
    }

    public Integer getAdjustment() {
        return adjustment;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }

    public boolean isRepair() {
        return repair;
    }

    public NetworkScaleDetails getStackNetworkScaleDetails() {
        return networkScaleDetails;
    }
}
