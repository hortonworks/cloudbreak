package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;

import com.azure.resourcemanager.compute.models.ResourceSkuCapabilities;
import com.sequenceiq.common.model.Architecture;

public class AzureVmCapabilities {

    private static final String ENCRYPTION_AT_HOST_SUPPORTED = "EncryptionAtHostSupported";

    private static final String ACCELERATED_NETWORKING_ENABLED = "AcceleratedNetworkingEnabled";

    private static final String CPU_ARCHITECTURE_TYPE = "CpuArchitectureType";

    private static final String AZURE_ARM64 = "Arm64";

    private static final String AZURE_X64 = "x64";

    private final String name;

    private boolean encryptionAtHostSupported;

    private boolean acceleratedNetworkingEnabled;

    private Architecture architecture = Architecture.X86_64;

    public AzureVmCapabilities(String name, List<ResourceSkuCapabilities> skuCapabilities) {
        this.name = name;
        if (skuCapabilities != null) {
            skuCapabilities.stream()
                    .filter(c -> ENCRYPTION_AT_HOST_SUPPORTED.equalsIgnoreCase(c.name()))
                    .findFirst().ifPresent(resourceSkuCapabilities -> encryptionAtHostSupported = Boolean.parseBoolean(resourceSkuCapabilities.value()));
            skuCapabilities.stream()
                    .filter(c -> ACCELERATED_NETWORKING_ENABLED.equalsIgnoreCase(c.name()))
                    .findFirst().ifPresent(resourceSkuCapabilities -> acceleratedNetworkingEnabled = Boolean.parseBoolean(resourceSkuCapabilities.value()));
            skuCapabilities.stream()
                    .filter(c -> CPU_ARCHITECTURE_TYPE.equalsIgnoreCase(c.name()))
                    .findFirst().ifPresent(resourceSkuCapabilities -> architecture = toArchitecture(resourceSkuCapabilities.value()));
        }
    }

    private static Architecture toArchitecture(String value) {
        if (AZURE_ARM64.equalsIgnoreCase(value)) {
            return Architecture.ARM64;
        } else if (AZURE_X64.equalsIgnoreCase(value)) {
            return Architecture.X86_64;
        }
        return Architecture.X86_64;
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

    public Architecture getArchitecture() {
        return architecture;
    }
}
