package com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset;

public class DiskEncryptionSetCreationCheckerContext {

    private final String resourceGroupName;

    private final String diskEncryptionSetName;

    private final boolean userManagedIdentityEnabled;

    public DiskEncryptionSetCreationCheckerContext(String resourceGroupName, String diskEncryptionSetName, boolean userManagedIdentityEnabled) {
        this.resourceGroupName = resourceGroupName;
        this.diskEncryptionSetName = diskEncryptionSetName;
        this.userManagedIdentityEnabled = userManagedIdentityEnabled;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public String getDiskEncryptionSetName() {
        return diskEncryptionSetName;
    }

    public boolean isUserManagedIdentityEnabled() {
        return userManagedIdentityEnabled;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DiskEncryptionSetCreationCheckerContext{");
        sb.append("resourceGroupName='").append(resourceGroupName).append('\'');
        sb.append(", diskEncryptionSetName='").append(diskEncryptionSetName).append('\'');
        sb.append(", userManagedIdentityEnabled='").append(userManagedIdentityEnabled).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
