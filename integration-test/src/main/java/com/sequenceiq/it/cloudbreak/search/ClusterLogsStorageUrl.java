package com.sequenceiq.it.cloudbreak.search;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderProxy;

@Component
public class ClusterLogsStorageUrl implements StorageUrl {

    @Override
    public String getFreeIpaStorageUrl(String resourceName, String resourceCrn, String baseLocation, CloudProviderProxy cloudProvider) {
        return cloudProvider.getCloudFunctionality().getFreeIpaLogsUrl(resourceName, resourceCrn, baseLocation);
    }

    @Override
    public String getDatalakeStorageUrl(String resourceName, String resourceCrn, String baseLocation, CloudProviderProxy cloudProvider) {
        return cloudProvider.getCloudFunctionality().getDataLakeLogsUrl(resourceName, resourceCrn, baseLocation);
    }

    @Override
    public String getDataHubStorageUrl(String resourceName, String resourceCrn, String baseLocation, CloudProviderProxy cloudProvider) {
        return cloudProvider.getCloudFunctionality().getDataHubLogsUrl(resourceName, resourceCrn, baseLocation);
    }
}

