package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.Optional;
import java.util.StringJoiner;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;

public class LaunchStackRequest extends CloudStackRequest<LaunchStackResult> {

    private final AdjustmentTypeWithThreshold adjustmentTypeWithThreshold;

    private final Optional<String> fallbackImage;

    public LaunchStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, AdjustmentType adjustmentType,
            Long threshold) {
        super(cloudContext, cloudCredential, cloudStack);
        this.adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(adjustmentType, threshold);
        this.fallbackImage = Optional.empty();
    }

    public LaunchStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, AdjustmentType adjustmentType,
            long threshold, Optional<String> fallbackImage) {
        super(cloudContext, cloudCredential, cloudStack);
        this.adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(adjustmentType, threshold);
        this.fallbackImage = fallbackImage;
    }

    public AdjustmentTypeWithThreshold getAdjustmentWithThreshold() {
        return adjustmentTypeWithThreshold;
    }

    public Optional<String> getFallbackImage() {
        return fallbackImage;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LaunchStackRequest.class.getSimpleName() + "[", "]")
                .add("adjustmentTypeWithThreshold=" + adjustmentTypeWithThreshold)
                .add("fallbackImage=" + fallbackImage)
                .toString();
    }
}