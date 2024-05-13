package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class CollectMetadataResult extends CloudPlatformResult implements FlowPayload {

    private final List<CloudVmMetaDataStatus> results;

    @JsonCreator
    public CollectMetadataResult(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("results") List<CloudVmMetaDataStatus> results) {
        super(resourceId);
        this.results = results;
    }

    public CollectMetadataResult(Exception errorDetails, Long resourceId) {
        super("", errorDetails, resourceId);
        this.results = null;
    }

    public List<CloudVmMetaDataStatus> getResults() {
        return results;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CollectMetadataResult{");
        sb.append(", results=").append(results);
        sb.append(", exception=").append(getErrorDetails());
        sb.append('}');
        return sb.toString();
    }
}