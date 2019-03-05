package com.sequenceiq.cloudbreak.template.filesystem.s3;

import java.util.Collection;

import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.services.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;

public class S3FileSystemConfigurationsView extends BaseFileSystemConfigurationsView {

    private String instanceProfile;

    public S3FileSystemConfigurationsView(S3FileSystem s3FileSystem, Collection<StorageLocationView> locations, boolean deafultFs) {
        super(s3FileSystem.getStorageContainer(), deafultFs, locations);
        instanceProfile = s3FileSystem.getInstanceProfile();
    }

    public String getInstanceProfile() {
        return instanceProfile;
    }

    public void setInstanceProfile(String instanceProfile) {
        this.instanceProfile = instanceProfile;
    }

    @Override
    public String getType() {
        return FileSystemType.S3.name();
    }
}
