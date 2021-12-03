package com.sequenceiq.freeipa.flow.freeipa.upscale.event;

import java.util.List;
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

    public UpscaleStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack stack, List<CloudResource> resourceList,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
        super(cloudContext, cloudCredential, stack);
        this.resourceList = resourceList;
        this.adjustmentTypeWithThreshold = adjustmentTypeWithThreshold;
    }

    public List<CloudResource> getResourceList() {
        return resourceList;
    }

    public AdjustmentTypeWithThreshold getAdjustmentWithThreshold() {
        return adjustmentTypeWithThreshold;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UpscaleStackRequest.class.getSimpleName() + "[", "]")
                .add("resourceList=" + resourceList)
                .add("adjustmentTypeWithThreshold=" + adjustmentTypeWithThreshold)
                .toString();
    }

}
