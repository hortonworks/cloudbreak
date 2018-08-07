package com.sequenceiq.cloudbreak.cloud.model.filesystem;

import java.util.Objects;

public class CloudWasbView extends CloudFileSystemView {

    private String accountKey;

    private String accountName;

    private boolean secure;

    private String resourceGroupName;

    public CloudWasbView() {
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
        return isSecure() == that.isSecure()
                && Objects.equals(getAccountKey(), that.getAccountKey())
                && Objects.equals(getAccountName(), that.getAccountName())
                && Objects.equals(getResourceGroupName(), that.getResourceGroupName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAccountKey(), getAccountName(), isSecure(), getResourceGroupName());
    }

}
