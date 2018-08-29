package com.sequenceiq.cloudbreak.template.filesystem.gcs;

import java.util.Collection;

import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.filesystem.GcsFileSystem;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;

public class GcsFileSystemConfigurationsView extends BaseFileSystemConfigurationsView {

    private String serviceAccountEmail;

    public GcsFileSystemConfigurationsView(GcsFileSystem gcsFileSystem, Collection<StorageLocationView> locations, boolean deafultFs) {
        super(gcsFileSystem.getStorageContainer(), deafultFs, locations);
        this.serviceAccountEmail = gcsFileSystem.getServiceAccountEmail();
    }

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    public void setServiceAccountEmail(String serviceAccountEmail) {
        this.serviceAccountEmail = serviceAccountEmail;
    }

    @Override
    public String getType() {
        return FileSystemType.GCS.name();
    }
}
