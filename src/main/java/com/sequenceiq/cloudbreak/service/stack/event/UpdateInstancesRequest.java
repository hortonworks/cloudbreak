package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.controller.json.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class UpdateInstancesRequest extends ProvisionEvent {

    private InstanceGroupAdjustmentJson instanceGroupAdjustmentJson;
    private Boolean withStackUpdate;


    public UpdateInstancesRequest(CloudPlatform cloudPlatform, Long stackId, InstanceGroupAdjustmentJson instanceGroupAdjustmentJson, Boolean withStackUpdate) {
        super(cloudPlatform, stackId);
        this.instanceGroupAdjustmentJson = instanceGroupAdjustmentJson;
        this.withStackUpdate = withStackUpdate;
    }

    public InstanceGroupAdjustmentJson getInstanceGroupAdjustmentJson() {
        return instanceGroupAdjustmentJson;
    }

    public Boolean isWithStackUpdate() {
        return withStackUpdate;
    }
}
