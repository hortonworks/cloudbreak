package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class StopStartDownscaleStopInstancesResult extends CloudPlatformResult {

    private final StopStartDownscaleStopInstancesRequest stopInstancesRequest;

    private final List<CloudVmInstanceStatus> affectedInstanceStatuses;

    public StopStartDownscaleStopInstancesResult(Long resourceId, StopStartDownscaleStopInstancesRequest request,
            List<CloudVmInstanceStatus> affectedInstanceStatuses) {
        super(resourceId);
        this.stopInstancesRequest = request;
        this.affectedInstanceStatuses = affectedInstanceStatuses;
    }

    public StopStartDownscaleStopInstancesRequest getStopInstancesRequest() {
        return stopInstancesRequest;
    }

    public List<CloudVmInstanceStatus> getAffectedInstanceStatuses() {
        return affectedInstanceStatuses;
    }
}
