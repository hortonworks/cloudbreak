package com.sequenceiq.cloudbreak.cloud.event.loadbalancer;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;

public class CollectLoadBalancerMetadataResult extends CloudPlatformResult {

    private List<CloudLoadBalancerMetadata> results;

    public CollectLoadBalancerMetadataResult(Long resourceId, List<CloudLoadBalancerMetadata> results) {
        super(resourceId);
        this.results = results;
    }

    public CollectLoadBalancerMetadataResult(Exception errorDetails, Long resourceId) {
        super("", errorDetails, resourceId);
    }

    public List<CloudLoadBalancerMetadata> getResults() {
        return results;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CollectLoadBalancerMetadataResult{");
        sb.append(", results=").append(results);
        sb.append(", exception=").append(getErrorDetails());
        sb.append('}');
        return sb.toString();
    }
}
