package com.sequenceiq.cloudbreak.template.filesystem.wasb;

import java.util.Collection;

import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.api.filesystem.WasbFileSystem;

public class WasbFileSystemConfigurationsView extends BaseFileSystemConfigurationsView {

    private String accountKey;

    private String accountName;

    private boolean secure;

    private String resourceGroupName;

    private String storageContainerName;

    public WasbFileSystemConfigurationsView(WasbFileSystem wasbFileSystem, Collection<StorageLocationView> locations, boolean defaultFs) {
        super(FileSystemType.WASB.name(), wasbFileSystem.getStorageContainer(), defaultFs, locations, null);
        accountName = wasbFileSystem.getAccountName();
        accountKey = wasbFileSystem.getAccountKey();
        secure = wasbFileSystem.isSecure();
        storageContainerName = wasbFileSystem.getStorageContainerName();
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

    public String getStorageContainerName() {
        return storageContainerName;
    }

    public void setStorageContainerName(String storageContainerName) {
        this.storageContainerName = storageContainerName;
    }

    @Override
    public String getProtocol() {
        return FileSystemType.WASB.getProtocol();
    }

}
