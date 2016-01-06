package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;

public class CollectMetadataResult extends CloudPlatformResult<CollectMetadataRequest> {
    private List<CloudVmMetaDataStatus> results;

    public CollectMetadataResult(CollectMetadataRequest request, List<CloudVmMetaDataStatus> results) {
        super(request);
        this.results = results;
    }

    public CollectMetadataResult(Exception errorDetails, CollectMetadataRequest request) {
        super("", errorDetails, request);
    }

    public List<CloudVmMetaDataStatus> getResults() {
        return results;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CollectMetadataResult{");
        sb.append("cloudContext=").append(getRequest().getCloudContext());
        sb.append(", results=").append(results);
        sb.append(", exception=").append(getErrorDetails());
        sb.append('}');
        return sb.toString();
    }
}
