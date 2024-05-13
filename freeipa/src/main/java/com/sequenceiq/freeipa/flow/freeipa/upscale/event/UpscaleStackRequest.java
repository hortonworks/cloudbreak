package com.sequenceiq.freeipa.flow.freeipa.upscale.event;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;

public class UpscaleStackRequest<T> extends CloudStackRequest<T> {

    private final List<CloudResource> resourceList;

    private final AdjustmentTypeWithThreshold adjustmentTypeWithThreshold;

    private final Optional<String> fallbackImage;

    public UpscaleStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack stack, List<CloudResource> resourceList,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, Optional<String> fallbackImage) {
        super(cloudContext, cloudCredential, stack);
        this.resourceList = resourceList;
        this.adjustmentTypeWithThreshold = adjustmentTypeWithThreshold;
        this.fallbackImage = fallbackImage;
    }

    public List<CloudResource> getResourceList() {
        return resourceList;
    }

    public AdjustmentTypeWithThreshold getAdjustmentWithThreshold() {
        return adjustmentTypeWithThreshold;
    }

    public Optional<String> getFallbackImage() {
        return fallbackImage;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UpscaleStackRequest.class.getSimpleName() + "[", "]")
                .add("resourceList=" + resourceList)
                .add("adjustmentTypeWithThreshold=" + adjustmentTypeWithThreshold)
                .add("fallbackImage=" + fallbackImage)
                .toString();
    }
}