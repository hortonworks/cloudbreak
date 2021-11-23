package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class StopStartDownscaleStopInstancesResult extends CloudPlatformResult {

    private final List<CloudInstance> cloudInstancesToStop;

    private final List<CloudVmInstanceStatus> cloudVmInstanceStatusesNoCheck;

    public StopStartDownscaleStopInstancesResult(Long resourceId, List<CloudInstance> cloudInstancesToStop, List<CloudVmInstanceStatus> cloudVmInstanceStatusesNoCheck) {
        super(resourceId);
        this.cloudInstancesToStop = cloudInstancesToStop;
        this.cloudVmInstanceStatusesNoCheck = cloudVmInstanceStatusesNoCheck;
    }

    public List<CloudInstance> getCloudInstancesToStop() {
        return cloudInstancesToStop;
    }

    // TODO CB-14929: This should include information about the actual instances that were STOPPED. The result needs to be filtered somewhewre for the actual status. Will evolve based on error handling.
    public List<CloudVmInstanceStatus> getCloudVmInstanceStatusesNoCheck() {
        return cloudVmInstanceStatusesNoCheck;
    }

    @Override
    public String toString() {
        return "StopStartDownscaleStopInstancesResult{" +
                "cloudInstancesToStop=" + cloudInstancesToStop +
                ", cloudVmInstanceStatusesNoCheck=" + cloudVmInstanceStatusesNoCheck +
                '}';
    }
}
