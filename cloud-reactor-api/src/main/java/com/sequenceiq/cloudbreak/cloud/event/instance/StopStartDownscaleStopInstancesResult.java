package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.Collections;
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

    public StopStartDownscaleStopInstancesResult(String statusReason, Exception errorDetails, Long resourceId,
            StopStartDownscaleStopInstancesRequest request) {
        super(statusReason, errorDetails, resourceId);
        this.stopInstancesRequest = request;
        this.affectedInstanceStatuses = Collections.emptyList();
    }

    public StopStartDownscaleStopInstancesRequest getStopInstancesRequest() {
        return stopInstancesRequest;
    }

    public List<CloudVmInstanceStatus> getAffectedInstanceStatuses() {
        return affectedInstanceStatuses;
    }

    @Override
    public String toString() {
        return "StopStartDownscaleStopInstancesResult{" +
                "stopInstancesRequest=" + stopInstancesRequest +
                ", affectedInstanceStatuses=" + affectedInstanceStatuses +
                '}';
    }
}
