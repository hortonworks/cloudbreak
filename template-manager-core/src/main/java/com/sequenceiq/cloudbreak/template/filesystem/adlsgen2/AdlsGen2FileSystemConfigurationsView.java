package com.sequenceiq.cloudbreak.template.filesystem.adlsgen2;

import java.util.Collection;

import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.common.api.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.common.model.FileSystemType;

public class AdlsGen2FileSystemConfigurationsView extends BaseFileSystemConfigurationsView {

    private String accountName;

    private String accountKey;

    private boolean secure;

    private String storageContainerName;

    private String storageContainer;

    public AdlsGen2FileSystemConfigurationsView(AdlsGen2FileSystem adlsGen2FileSystem, Collection<StorageLocationView> locations, boolean deafultFs) {
        super(FileSystemType.ADLS_GEN_2.name(), adlsGen2FileSystem.getStorageContainer(), deafultFs, locations);
        accountName = adlsGen2FileSystem.getAccountName();
        accountKey = adlsGen2FileSystem.getAccountKey();
        storageContainerName = adlsGen2FileSystem.getStorageContainerName();
        storageContainer = adlsGen2FileSystem.getStorageContainer();
        secure = adlsGen2FileSystem.isSecure();
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

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    @Override
    public String getProtocol() {
        return FileSystemType.ADLS_GEN_2.getProtocol();
    }
}
