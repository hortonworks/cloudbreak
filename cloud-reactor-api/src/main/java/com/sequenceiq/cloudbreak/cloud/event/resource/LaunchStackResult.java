package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;

public class LaunchStackResult extends CloudPlatformResult {
    private List<CloudResourceStatus> results;

    public LaunchStackResult(Long resourceId, List<CloudResourceStatus> results) {
        super(resourceId);
        this.results = results;
    }

    public LaunchStackResult(Exception errorDetails, Long resourceId) {
        super("", errorDetails, resourceId);
    }

    public List<CloudResourceStatus> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "LaunchStackResult{"
                + "status=" + getStatus()
                + ", statusReason='" + getStatusReason() + '\''
                + ", errorDetails=" + getErrorDetails()
                + ", results=" + results
                + '}';
    }
}
