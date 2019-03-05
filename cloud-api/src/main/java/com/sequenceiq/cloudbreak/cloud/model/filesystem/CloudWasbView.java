package com.sequenceiq.cloudbreak.cloud.model.filesystem;

import java.util.Objects;

public class CloudWasbView extends CloudFileSystemView {

    private String accountKey;

    private String accountName;

    private boolean secure;

    private String resourceGroupName;

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

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CloudWasbView)) {
            return false;
        }
        CloudWasbView that = (CloudWasbView) o;
        return secure == that.secure
                && Objects.equals(accountKey, that.accountKey)
                && Objects.equals(accountName, that.accountName)
                && Objects.equals(resourceGroupName, that.resourceGroupName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountKey, accountName, secure, resourceGroupName);
    }

}
