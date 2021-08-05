package com.sequenceiq.it.cloudbreak.search;

import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderProxy;

public interface StorageUrl {
    String getFreeIpaStorageUrl(String resourceName, String resourceCrn, String baseLocation, CloudProviderProxy cloudProvider);

    String getDatalakeStorageUrl(String resourceName, String resourceCrn, String baseLocation, CloudProviderProxy cloudProvider);

    String getDataHubStorageUrl(String resourceName, String resourceCrn, String baseLocation, CloudProviderProxy cloudProvider);
}

