package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;

public class CloudNetworkCreationResult extends CloudPlatformResult {

    private CreatedCloudNetwork createdCloudNetwork;

    public CloudNetworkCreationResult(CloudPlatformRequest<?> request, CreatedCloudNetwork createdCloudNetwork) {
        super(request.getResourceId());
        this.createdCloudNetwork = createdCloudNetwork;
    }

    public CloudNetworkCreationResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request.getResourceId());
    }

    public CreatedCloudNetwork getCreatedCloudNetwork() {
        return createdCloudNetwork;
    }
}
