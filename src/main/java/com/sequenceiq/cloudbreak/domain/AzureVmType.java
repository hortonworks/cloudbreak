package com.sequenceiq.cloudbreak.domain;

public enum AzureVmType {

    EXTRA_SMALL("Extra Small"),
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large"),
    EXTRA_LARGE("Extra Large");

    private final String vmType;

    private AzureVmType(String vmType) {
        this.vmType = vmType;
    }

    public String vmType() {
        return this.vmType;
    }
}
