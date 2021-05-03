package com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset;

public class DiskEncryptionSetCreationCheckerContext {

    private final String resourceGroupName;

    private final String diskEncryptionSetName;

    public DiskEncryptionSetCreationCheckerContext(String resourceGroupName, String diskEncryptionSetName) {
        this.resourceGroupName = resourceGroupName;
        this.diskEncryptionSetName = diskEncryptionSetName;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public String getDiskEncryptionSetName() {
        return diskEncryptionSetName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DiskEncryptionSetCreationCheckerContext{");
        sb.append("resourceGroupName='").append(resourceGroupName).append('\'');
        sb.append(", diskEncryptionSetName='").append(diskEncryptionSetName).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
