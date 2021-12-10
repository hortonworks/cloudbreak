package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class StopStartUpscaleStartInstancesResult extends CloudPlatformResult {

    private final StopStartUpscaleStartInstancesRequest startInstanceRequest;

    private final List<CloudVmInstanceStatus> affectedInstanceStatuses;

    public StopStartUpscaleStartInstancesResult(Long resourceId, StopStartUpscaleStartInstancesRequest request,
            List<CloudVmInstanceStatus> affectedInstanceStatuses) {
        super(resourceId);
        this.startInstanceRequest = request;
        this.affectedInstanceStatuses = affectedInstanceStatuses;
    }

    public StopStartUpscaleStartInstancesRequest getStartInstanceRequest() {
        return startInstanceRequest;
    }

    public List<CloudVmInstanceStatus> getAffectedInstanceStatuses() {
        return affectedInstanceStatuses;
    }
}