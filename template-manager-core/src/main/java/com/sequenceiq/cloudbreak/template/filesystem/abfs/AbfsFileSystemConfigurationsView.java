package com.sequenceiq.cloudbreak.template.filesystem.abfs;

import java.util.Collection;

import com.sequenceiq.cloudbreak.api.model.filesystem.AbfsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;

public class AbfsFileSystemConfigurationsView extends BaseFileSystemConfigurationsView {

    private String accountName;

    private String accountKey;

    private String storageContainerName;

    private String storageContainer;

    public AbfsFileSystemConfigurationsView(AbfsFileSystem abfsFileSystem, Collection<StorageLocationView> locations, boolean deafultFs) {
        super(abfsFileSystem.getStorageContainer(), deafultFs, locations);
        this.accountName = abfsFileSystem.getAccountName();
        this.accountKey = abfsFileSystem.getAccountKey();
        this.storageContainerName = abfsFileSystem.getStorageContainerName();
        this.storageContainer = abfsFileSystem.getStorageContainer();
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountKey() {
        return accountKey;
    }

    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
    }

    public String getStorageContainerName() {
        return storageContainerName;
    }

    public void setStorageContainerName(String storageContainerName) {
        this.storageContainerName = storageContainerName;
    }

    @Override
    public String getStorageContainer() {
        return storageContainer;
    }

    @Override
    public void setStorageContainer(String storageContainer) {
        this.storageContainer = storageContainer;
    }

    @Override
    public String getType() {
        return FileSystemType.ABFS.name();
    }
}
