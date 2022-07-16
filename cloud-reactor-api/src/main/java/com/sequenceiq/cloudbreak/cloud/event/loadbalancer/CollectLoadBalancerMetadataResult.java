package com.sequenceiq.cloudbreak.cloud.event.loadbalancer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class CollectLoadBalancerMetadataResult extends CloudPlatformResult implements FlowPayload {

    private final List<CloudLoadBalancerMetadata> results;

    @JsonCreator
    public CollectLoadBalancerMetadataResult(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("results") List<CloudLoadBalancerMetadata> results) {
        super(resourceId);
        this.results = results;
    }

    public CollectLoadBalancerMetadataResult(Exception errorDetails, Long resourceId) {
        super("", errorDetails, resourceId);
        this.results = null;
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
