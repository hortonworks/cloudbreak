package com.sequenceiq.cloudbreak.cloud.model.filesystem;

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
}
