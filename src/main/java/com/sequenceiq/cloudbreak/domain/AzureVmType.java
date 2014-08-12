package com.sequenceiq.cloudbreak.domain;

public enum AzureVmType {

    EXTRA_SMALL("Extra Small", 1),
    SMALL("Small", 2),
    MEDIUM("Medium", 4),
    LARGE("Large", 8),
    EXTRA_LARGE("Extra Large", 16);

    private final String vmType;
    private final int maxDiskSize;

    private AzureVmType(String vmType, int maxDiskSize) {
        this.vmType = vmType;
        this.maxDiskSize = maxDiskSize;
    }

    public String vmType() {
        return this.vmType;
    }

    public int maxDiskSize() {
        return this.maxDiskSize;
    }
}
