package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.wasbintegrated;

public class StorageAccountCheckerContext {
    private String tenantId;

    private String subscriptionId;

    private String appId;

    private String appPassword;

    private String storageAccountName;

    private String resourceGroupName;

    public StorageAccountCheckerContext(String tenantId, String subscriptionId, String appId, String appPassword, String storageAccountName,
            String resourceGroupName) {
        this.tenantId = tenantId;
        this.subscriptionId = subscriptionId;
        this.appId = appId;
        this.appPassword = appPassword;
        this.storageAccountName = storageAccountName;
        this.resourceGroupName = resourceGroupName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppPassword() {
        return appPassword;
    }

    public String getStorageAccountName() {
        return storageAccountName;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }
}
