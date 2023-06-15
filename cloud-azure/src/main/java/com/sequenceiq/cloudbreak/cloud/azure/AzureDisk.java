package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Map;

import com.google.common.base.Objects;

public class AzureDisk {
    private String diskName;

    private int diskSize;

    private AzureDiskType diskType;

    private String region;

    private String resourceGroupName;

    private Map<String, String> tags;

    private String diskEncryptionSetId;

    private String availabilityZone;

    public AzureDisk(String diskName, int diskSize, AzureDiskType diskType, String region, String resourceGroupName,
            Map<String, String> tags, String diskEncryptionSetId, String availabilityZone) {
        this.diskName = diskName;
        this.diskSize = diskSize;
        this.diskType = diskType;
        this.region = region;
        this.resourceGroupName = resourceGroupName;
        this.tags = tags;
        this.diskEncryptionSetId = diskEncryptionSetId;
        this.availabilityZone = availabilityZone;
    }

    public String getDiskName() {
        return diskName;
    }

    public void setDiskName(String diskName) {
        this.diskName = diskName;
    }

    public int getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(int diskSize) {
        this.diskSize = diskSize;
    }

    public AzureDiskType getDiskType() {
        return diskType;
    }

    public void setDiskType(AzureDiskType diskType) {
        this.diskType = diskType;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public String getDiskEncryptionSetId() {
        return diskEncryptionSetId;
    }

    public void setDiskEncryptionSetId(String diskEncryptionSetId) {
        this.diskEncryptionSetId = diskEncryptionSetId;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    @Override
    public String toString() {
        return "AzureDisk{" +
                "diskName='" + diskName + '\'' +
                ", diskSize='" + diskSize + '\'' +
                ", diskType=" + diskType +
                ", region='" + region + '\'' +
                ", resourceGroupName='" + resourceGroupName + '\'' +
                ", tags=" + tags +
                ", diskEncryptionSetId='" + diskEncryptionSetId + '\'' +
                ", availabilityZone='" + availabilityZone + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AzureDisk azureDisk = (AzureDisk) o;
        return Objects.equal(diskName, azureDisk.diskName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(diskName);
    }
}
