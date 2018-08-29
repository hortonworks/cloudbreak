package com.sequenceiq.cloudbreak.template.views;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.StorageLocations;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;

public class FileSystemConfigurationView {

    private final BaseFileSystemConfigurationsView fileSystemConfiguration;

    private final boolean defaultFs;

    private final Set<StorageLocationView> locations;

    public FileSystemConfigurationView(BaseFileSystemConfigurationsView fileSystemConfiguration, StorageLocations storageLocations) {
        this.fileSystemConfiguration = fileSystemConfiguration;
        Set<StorageLocationView> storageLocationViews = new HashSet<>();
        if (storageLocations == null || storageLocations.getLocations() == null) {
            this.locations = new HashSet<>();
        } else {
            for (StorageLocation storageLocation : storageLocations.getLocations()) {
                storageLocationViews.add(new StorageLocationView(storageLocation));
            }
            this.locations = storageLocationViews;
        }
        this.defaultFs = false;
    }

    public BaseFileSystemConfigurationsView getFileSystemConfiguration() {
        return fileSystemConfiguration;
    }

    public boolean isDefaultFs() {
        return defaultFs;
    }

    public Set<StorageLocationView> getLocations() {
        return locations;
    }
}
