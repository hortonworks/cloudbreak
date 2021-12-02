package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class StopStartUpscaleStartInstancesResult extends CloudPlatformResult {

    // TODO CB-14929: Make sure someone processes the CloudVMInstanceStatus to determine which instances were actually started.
    private final List<CloudVmInstanceStatus> startedInstances;

    public StopStartUpscaleStartInstancesResult(Long resourceId, List<CloudVmInstanceStatus> startedInstances) {
        super(resourceId);
        this.startedInstances = startedInstances;
    }

    public List<CloudVmInstanceStatus> getStartedInstances() {
        return startedInstances;
    }
}