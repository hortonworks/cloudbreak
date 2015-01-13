package com.sequenceiq.cloudbreak.domain;

public enum AzureVmType {

    EXTRA_SMALL("Extra Small", 1),
    SMALL("Small", 2),
    MEDIUM("Medium", 4),
    LARGE("Large", 8),
    EXTRA_LARGE("Extra Large", 16),
    A5("A5", 4),
    A6("A6", 4),
    A7("A7", 8),
    A8("A8", 8),
    A9("A9", 16),
    BASIC_A0("Basic_A0", 1),
    BASIC_A1("Basic_A1", 2),
    BASIC_A2("Basic_A2", 4),
    BASIC_A3("Basic_A3", 8),
    BASIC_A4("Basic_A4", 16),
    STANDARD_G1("Standard_G1", 1),
    STANDARD_G2("Standard_G2", 2),
    STANDARD_G3("Standard_G3", 4),
    STANDARD_G4("Standard_G4", 8),
    STANDARD_G5("Standard_G5", 16),
    STANDARD_D1("Standard_D1", 2),
    STANDARD_D2("Standard_D2", 4),
    STANDARD_D3("Standard_D3", 8),
    STANDARD_D4("Standard_D4", 16),
    STANDARD_D11("Standard_D11", 4),
    STANDARD_D12("Standard_D12", 8),
    STANDARD_D13("Standard_D13", 16),
    STANDARD_D14("Standard_D14", 32);

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
