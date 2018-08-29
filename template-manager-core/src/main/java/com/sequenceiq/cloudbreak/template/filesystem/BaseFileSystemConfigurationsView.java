package com.sequenceiq.cloudbreak.template.filesystem;

import java.util.Collection;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

public abstract class BaseFileSystemConfigurationsView implements ProvisionEntity {

    private String storageContainer;

    private boolean defaultFs;

    private Collection<StorageLocationView> locations;

    public BaseFileSystemConfigurationsView(String storageContainer, boolean defaultFs, Collection<StorageLocationView> locations) {
        this.storageContainer = storageContainer;
        this.defaultFs = defaultFs;
        this.locations = locations;
    }

    public String getStorageContainer() {
        return storageContainer;
    }

    public void setStorageContainer(String storageContainer) {
        this.storageContainer = storageContainer;
    }

    public boolean isDefaultFs() {
        return defaultFs;
    }

    public Collection<StorageLocationView> getLocations() {
        return locations;
    }

    public abstract String getType();
}

