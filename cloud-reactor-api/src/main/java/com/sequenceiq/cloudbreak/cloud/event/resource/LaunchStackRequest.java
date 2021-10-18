package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;

public class LaunchStackRequest extends CloudStackRequest<LaunchStackResult> {

    private final AdjustmentTypeWithThreshold adjustmentTypeWithThreshold;

    public LaunchStackRequest(CloudContext cloudCtx, CloudCredential cloudCredential, CloudStack cloudStack, AdjustmentType adjustmentType, Long threshold) {
        super(cloudCtx, cloudCredential, cloudStack);
        this.adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(adjustmentType, threshold);
    }

    public AdjustmentTypeWithThreshold getAdjustmentWithThreshold() {
        return adjustmentTypeWithThreshold;
    }
}
