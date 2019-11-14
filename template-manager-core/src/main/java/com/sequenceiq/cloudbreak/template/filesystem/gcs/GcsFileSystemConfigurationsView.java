package com.sequenceiq.cloudbreak.template.filesystem.gcs;

import java.util.Collection;

import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.api.filesystem.GcsFileSystem;

public class GcsFileSystemConfigurationsView extends BaseFileSystemConfigurationsView {

    private String serviceAccountEmail;

    public GcsFileSystemConfigurationsView(GcsFileSystem gcsFileSystem, Collection<StorageLocationView> locations, boolean defaultFs) {
        super(FileSystemType.GCS.name(), gcsFileSystem.getStorageContainer(), defaultFs, locations, null);
        serviceAccountEmail = gcsFileSystem.getServiceAccountEmail();
    }

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    public void setServiceAccountEmail(String serviceAccountEmail) {
        this.serviceAccountEmail = serviceAccountEmail;
    }

    @Override
    public String getProtocol() {
        return FileSystemType.GCS.getProtocol();
    }
}
