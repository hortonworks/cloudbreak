package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Collections;
import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class UnhealthyInstancesDetectionResult extends ClusterPlatformResult<UnhealthyInstancesDetectionRequest> {

    private final Set<String> unhealthyInstanceIds;

    public UnhealthyInstancesDetectionResult(UnhealthyInstancesDetectionRequest request, Set<String> unhealthyInstanceIds) {
        super(request);
        this.unhealthyInstanceIds = unhealthyInstanceIds;
    }

    public UnhealthyInstancesDetectionResult(String statusReason, Exception errorDetails, UnhealthyInstancesDetectionRequest request) {
        super(statusReason, errorDetails, request);
        unhealthyInstanceIds = Collections.emptySet();
    }

    public Set<String> getUnhealthyInstanceIds() {
        return unhealthyInstanceIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UnhealthyInstancesDetectionResult that = (UnhealthyInstancesDetectionResult) o;

        return unhealthyInstanceIds.equals(that.unhealthyInstanceIds);

    }

    @Override
    public int hashCode() {
        return unhealthyInstanceIds.hashCode();
    }
}
