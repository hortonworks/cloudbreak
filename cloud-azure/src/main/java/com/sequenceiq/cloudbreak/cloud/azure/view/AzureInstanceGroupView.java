package com.sequenceiq.cloudbreak.cloud.azure.view;

public class AzureInstanceGroupView {

    private String name;

    private String compressedName;

    private Integer platformFaultDomainCount;

    private Integer platformUpdateDomainCount;

    private String availabilitySetName;

    private boolean managedDisk;

    public AzureInstanceGroupView(String name) {
        this.name = name;
        this.compressedName = name.replaceAll("_", "");
    }

    public AzureInstanceGroupView(String name, Integer platformFaultDomainCount, Integer platformUpdateDomainCount, String availabilitySetName) {
        this.name = name;
        this.platformFaultDomainCount = platformFaultDomainCount;
        this.platformUpdateDomainCount = platformUpdateDomainCount;
        this.availabilitySetName = availabilitySetName;
        this.compressedName = name.replaceAll("_", "");
    }

    public String getName() {
        return name;
    }

    public Integer getPlatformFaultDomainCount() {
        return platformFaultDomainCount;
    }

    public void setPlatformFaultDomainCount(Integer platformFaultDomainCount) {
        this.platformFaultDomainCount = platformFaultDomainCount;
    }

    public Integer getPlatformUpdateDomainCount() {
        return platformUpdateDomainCount;
    }

    public void setPlatformUpdateDomainCount(Integer platformUpdateDomainCount) {
        this.platformUpdateDomainCount = platformUpdateDomainCount;
    }

    public String getAvailabilitySetName() {
        return availabilitySetName;
    }

    public void setAvailabilitySetName(String availabilitySetName) {
        this.availabilitySetName = availabilitySetName;
    }

    public boolean isManagedDisk() {
        return managedDisk;
    }

    public void setManagedDisk(boolean managedDisk) {
        this.managedDisk = managedDisk;
    }

    public String getCompressedName() {
        return compressedName;
    }
}
