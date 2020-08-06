package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;

public class GetPlatformNetworksResult extends CloudPlatformResult {
    private CloudNetworks cloudNetworks;

    public GetPlatformNetworksResult(Long resourceId, CloudNetworks cloudNetworks) {
        super(resourceId);
        this.cloudNetworks = cloudNetworks;
    }

    public GetPlatformNetworksResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CloudNetworks getCloudNetworks() {
        return cloudNetworks;
    }

    @Override
    public String toString() {
        return "GetPlatformNetworksResult{" +
                "cloudNetworks=" + cloudNetworks +
                '}';
    }
}
