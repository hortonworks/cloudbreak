package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.type.AdjustmentType;

public class LaunchStackRequest extends CloudStackRequest<LaunchStackResult> {

    private final AdjustmentType adjustmentType;
    private final Long threshold;

    public LaunchStackRequest(CloudContext cloudCtx, CloudCredential cloudCredential, CloudStack cloudStack, AdjustmentType adjustmentType, Long threshold) {
        super(cloudCtx, cloudCredential, cloudStack);
        this.adjustmentType = adjustmentType;
        this.threshold = threshold;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public Long getThreshold() {
        return threshold;
    }
}
