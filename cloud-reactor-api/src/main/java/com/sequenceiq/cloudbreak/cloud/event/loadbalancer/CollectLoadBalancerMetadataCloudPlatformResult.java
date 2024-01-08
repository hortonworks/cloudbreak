package com.sequenceiq.cloudbreak.cloud.event.loadbalancer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;

public class CollectLoadBalancerMetadataCloudPlatformResult extends CloudPlatformResult {

    private final List<CloudLoadBalancerMetadata> results;

    @JsonCreator
    public CollectLoadBalancerMetadataCloudPlatformResult(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("results") List<CloudLoadBalancerMetadata> results) {
        super(resourceId);
        this.results = results;
    }

    public CollectLoadBalancerMetadataCloudPlatformResult(Exception errorDetails, Long resourceId) {
        super("", errorDetails, resourceId);
        this.results = null;
    }

    public List<CloudLoadBalancerMetadata> getResults() {
        return results;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CollectLoadBalancerMetadataCloudPlatformResult{");
        sb.append(", results=").append(results);
        sb.append(", exception=").append(getErrorDetails());
        sb.append('}');
        return sb.toString();
    }
}
