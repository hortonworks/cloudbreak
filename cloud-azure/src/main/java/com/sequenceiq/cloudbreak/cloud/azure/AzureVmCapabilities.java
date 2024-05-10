package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;

import com.azure.resourcemanager.compute.models.ResourceSkuCapabilities;

public class AzureVmCapabilities {

    private static final String ENCRYPTION_AT_HOST_SUPPORTED = "EncryptionAtHostSupported";

    private static final String ACCELERATED_NETWORKING_ENABLED = "AcceleratedNetworkingEnabled";

    private final String name;

    private boolean encryptionAtHostSupported;

    private boolean acceleratedNetworkingEnabled;

    public AzureVmCapabilities(String name, List<ResourceSkuCapabilities> skuCapabilities) {
        this.name = name;
        if (skuCapabilities != null) {
            skuCapabilities.stream()
                    .filter(c -> ENCRYPTION_AT_HOST_SUPPORTED.equalsIgnoreCase(c.name()))
                    .findFirst().ifPresent(resourceSkuCapabilities -> encryptionAtHostSupported = Boolean.parseBoolean(resourceSkuCapabilities.value()));
            skuCapabilities.stream()
                    .filter(c -> ACCELERATED_NETWORKING_ENABLED.equalsIgnoreCase(c.name()))
                    .findFirst().ifPresent(resourceSkuCapabilities -> acceleratedNetworkingEnabled = Boolean.parseBoolean(resourceSkuCapabilities.value()));
        }
    }

    public String getName() {
        return name;
    }

    public boolean isEncryptionAtHostSupported() {
        return encryptionAtHostSupported;
    }

    public boolean isAcceleratedNetworkingEnabled() {
        return acceleratedNetworkingEnabled;
    }
}
