package com.sequenceiq.cloudbreak.common.type;

public enum AzureVmType {

    A5("Standard_A5", 4),
    A6("Standard_A6", 8),
    A7("Standard_A7", 16),
    A8("Standard_A8", 16),
    A9("Standard_A9", 16),
    A10("Standard_A10", 16),
    A11("Standard_A11", 16),

    STANDARD_G1("Standard_G1", 4),
    STANDARD_G2("Standard_G2", 8),
    STANDARD_G3("Standard_G3", 16),
    STANDARD_G4("Standard_G4", 32),
    STANDARD_G5("Standard_G5", 64),

    STANDARD_D1("Standard_D1", 2),
    STANDARD_D2("Standard_D2", 4),
    STANDARD_D3("Standard_D3", 8),
    STANDARD_D4("Standard_D4", 16),
    STANDARD_D11("Standard_D11", 4),
    STANDARD_D12("Standard_D12", 8),
    STANDARD_D13("Standard_D13", 16),
    STANDARD_D14("Standard_D14", 32),

    STANDARD_D1_V2("Standard_D1_v2", 2),
    STANDARD_D2_V2("Standard_D2_v2", 4),
    STANDARD_D3_V2("Standard_D3_v2", 8),
    STANDARD_D4_V2("Standard_D4_v2", 16),
    STANDARD_D5_V2("Standard_D5_v2", 32),
    STANDARD_D11_V2("Standard_D11_v2", 4),
    STANDARD_D12_V2("Standard_D12_v2", 8),
    STANDARD_D13_V2("Standard_D13_v2", 16),
    STANDARD_D14_V2("Standard_D14_v2", 32);

    private final String vmType;
    private final int maxDiskCount;

    private AzureVmType(String vmType, int maxDiskCount) {
        this.vmType = vmType;
        this.maxDiskCount = maxDiskCount;
    }

    public String vmType() {
        return this.vmType;
    }

    public int maxDiskCount() {
        return this.maxDiskCount;
    }
}
