package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;

public class CollectMetadataResult extends CloudPlatformResult {
    private List<CloudVmMetaDataStatus> results;

    public CollectMetadataResult(Long resourceId, List<CloudVmMetaDataStatus> results) {
        super(resourceId);
        this.results = results;
    }

    public CollectMetadataResult(Exception errorDetails, Long resourceId) {
        super("", errorDetails, resourceId);
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
