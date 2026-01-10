package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;

public class UpscaleStackRequest<T> extends CloudStackRequest<T> {

    private final List<CloudResource> resourceList;

    private final AdjustmentTypeWithThreshold adjustmentTypeWithThreshold;

    private final boolean migrationNeed;

    public UpscaleStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack stack, List<CloudResource> resourceList,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, boolean migrationNeed) {
        super(cloudContext, cloudCredential, stack);
        this.resourceList = resourceList;
        this.adjustmentTypeWithThreshold = adjustmentTypeWithThreshold;
        this.migrationNeed = migrationNeed;
    }

    public List<CloudResource> getResourceList() {
        return resourceList;
    }

    public AdjustmentTypeWithThreshold getAdjustmentWithThreshold() {
        return adjustmentTypeWithThreshold;
    }

    public boolean isMigrationNeed() {
        return migrationNeed;
    }

    @Override
    public String toString() {
        return "UpscaleStackRequest{" +
                "migrationNeed=" + migrationNeed +
                ", adjustmentTypeWithThreshold=" + adjustmentTypeWithThreshold +
                ", resourceList=" + resourceList +
                "} " + super.toString();
    }
}