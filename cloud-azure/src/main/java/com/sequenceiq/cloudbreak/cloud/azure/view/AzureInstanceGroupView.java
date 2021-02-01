package com.sequenceiq.cloudbreak.cloud.azure.view;

public class AzureInstanceGroupView {

    private final String name;

    private final String compressedName;

    private final Integer rootVolumeSize;

    private Integer platformFaultDomainCount;

    private Integer platformUpdateDomainCount;

    private String availabilitySetName;

    private boolean managedDisk;

    public AzureInstanceGroupView(String name, Integer rootVolumeSize) {
        this.name = name;
        compressedName = name.replaceAll("_", "");
        this.rootVolumeSize = rootVolumeSize;
    }

    public AzureInstanceGroupView(String name, Integer platformFaultDomainCount, Integer platformUpdateDomainCount,
            String availabilitySetName, Integer rootVolumeSize) {
        this.name = name;
        this.platformFaultDomainCount = platformFaultDomainCount;
        this.platformUpdateDomainCount = platformUpdateDomainCount;
        this.availabilitySetName = availabilitySetName;
        compressedName = name.replaceAll("_", "");
        this.rootVolumeSize = rootVolumeSize;
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

    public Integer getRootVolumeSize() {
        return rootVolumeSize;
    }

    /**
     * needed because of Freemarker template generating (1.6 {@literal ->} 1.16 compatibility)
     *
     * @return name of the instance group
     */
    @Override
    public String toString() {
        return name;
    }
}
