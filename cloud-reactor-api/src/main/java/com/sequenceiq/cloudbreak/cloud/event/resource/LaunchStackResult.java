package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;

public class LaunchStackResult extends CloudPlatformResult<CloudPlatformRequest> {
    private List<CloudResourceStatus> results;

    public LaunchStackResult(CloudPlatformRequest<?> request, List<CloudResourceStatus> results) {
        super(request);
        this.results = results;
    }

    public LaunchStackResult(Exception errorDetails, CloudPlatformRequest<?> request) {
        super("", errorDetails, request);
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
                + ", request=" + getRequest()
                + ", results=" + results
                + '}';
    }
}
