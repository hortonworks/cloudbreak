package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.flow.core.FlowParameters;

public class StackScalingFlowContext extends StackContext {

    private final Map<String, Integer> hostGroupWithAdjustment;

    private final Map<String, Set<Long>> hostGroupWithPrivateIds;

    private final Map<String, Set<String>> hostgroupWithHostnames;

    private final boolean repair;

    private final NetworkScaleDetails networkScaleDetails;

    private final AdjustmentTypeWithThreshold adjustmentTypeWithThreshold;

    public StackScalingFlowContext(FlowParameters flowParameters, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential,
            CloudStack cloudStack, Map<String, Integer> hostGroupWithAdjustment, Map<String, Set<Long>> hostGroupWithPrivateIds,
            Map<String, Set<String>> hostgroupWithHostnames, boolean repair, AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
        this(flowParameters, stack, cloudContext, cloudCredential, cloudStack, hostGroupWithAdjustment, hostGroupWithPrivateIds, hostgroupWithHostnames, repair,
                NetworkScaleDetails.getEmpty(), adjustmentTypeWithThreshold);
    }

    public StackScalingFlowContext(FlowParameters flowParameters, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential,
            CloudStack cloudStack, Map<String, Integer> hostGroupWithAdjustment, Map<String, Set<Long>> hostGroupWithPrivateIds,
            Map<String, Set<String>> hostgroupWithHostnames, boolean repair, NetworkScaleDetails networkScaleDetails,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
        super(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
        this.hostGroupWithAdjustment = hostGroupWithAdjustment;
        this.hostGroupWithPrivateIds = hostGroupWithPrivateIds;
        this.hostgroupWithHostnames = hostgroupWithHostnames;
        this.repair = repair;
        this.networkScaleDetails = networkScaleDetails;
        this.adjustmentTypeWithThreshold = adjustmentTypeWithThreshold;
    }

    public Map<String, Integer> getHostGroupWithAdjustment() {
        return hostGroupWithAdjustment;
    }

    public Map<String, Set<Long>> getHostGroupWithPrivateIds() {
        return hostGroupWithPrivateIds;
    }

    public Map<String, Set<String>> getHostgroupWithHostnames() {
        return hostgroupWithHostnames;
    }

    public boolean isRepair() {
        return repair;
    }

    public AdjustmentTypeWithThreshold getAdjustmentTypeWithThreshold() {
        return adjustmentTypeWithThreshold;
    }

    public NetworkScaleDetails getStackNetworkScaleDetails() {
        return networkScaleDetails;
    }
}
