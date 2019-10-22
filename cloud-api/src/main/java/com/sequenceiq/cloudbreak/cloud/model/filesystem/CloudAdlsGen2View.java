package com.sequenceiq.cloudbreak.cloud.model.filesystem;

import java.util.Objects;

import com.sequenceiq.common.model.CloudIdentityType;

public class CloudAdlsGen2View extends CloudFileSystemView {

    private String accountKey;

    private String accountName;

    private String resourceGroupName;

    private boolean secure;

    private String managedIdentity;

    public CloudAdlsGen2View(CloudIdentityType cloudIdentityType) {
        super(cloudIdentityType);
    }

    public String getAccountKey() {
        return accountKey;
    }

    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getManagedIdentity() {
        return managedIdentity;
    }

    public void setManagedIdentity(String managedIdentity) {
        this.managedIdentity = managedIdentity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CloudAdlsGen2View that = (CloudAdlsGen2View) o;
        return secure == that.secure &&
                Objects.equals(accountKey, that.accountKey) &&
                Objects.equals(accountName, that.accountName) &&
                Objects.equals(resourceGroupName, that.resourceGroupName) &&
                Objects.equals(managedIdentity, that.managedIdentity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountKey, accountName, resourceGroupName, managedIdentity);
    }

}
