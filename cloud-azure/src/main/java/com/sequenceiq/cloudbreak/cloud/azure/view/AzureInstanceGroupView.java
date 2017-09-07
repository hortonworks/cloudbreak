package com.sequenceiq.cloudbreak.cloud.azure.view;

public class AzureInstanceGroupView {

    private final String name;

    private final String compressedName;

    private Integer platformFaultDomainCount;

    private Integer platformUpdateDomainCount;

    private String availabilitySetName;

    private boolean managedDisk;

    public AzureInstanceGroupView(String name) {
        this.name = name;
        compressedName = name.replaceAll("_", "");
    }

    public AzureInstanceGroupView(String name, Integer platformFaultDomainCount, Integer platformUpdateDomainCount, String availabilitySetName) {
        this.name = name;
        this.platformFaultDomainCount = platformFaultDomainCount;
        this.platformUpdateDomainCount = platformUpdateDomainCount;
        this.availabilitySetName = availabilitySetName;
        compressedName = name.replaceAll("_", "");
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

    /**
     * needed because of Freemarker template generating (1.6 -> 1.16 compatibility)
     *
     * @return name of the instance group
     */
    @Override
    public String toString() {
        return name;
    }
}
