package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;

public class GetPlatformNetworksResult extends CloudPlatformResult<CloudPlatformRequest> {
    private CloudNetworks cloudNetworks;

    public GetPlatformNetworksResult(CloudPlatformRequest<?> request, CloudNetworks cloudNetworks) {
        super(request);
        this.cloudNetworks = cloudNetworks;
    }

    public GetPlatformNetworksResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public CloudNetworks getCloudNetworks() {
        return cloudNetworks;
    }
}
