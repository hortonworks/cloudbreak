package com.sequenceiq.cloudbreak.template.filesystem.s3;

import java.util.Collection;

import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.api.filesystem.S3FileSystem;

public class S3FileSystemConfigurationsView extends BaseFileSystemConfigurationsView {

    private String instanceProfile;

    public S3FileSystemConfigurationsView(S3FileSystem s3FileSystem, Collection<StorageLocationView> locations, boolean deafultFs) {
        super(FileSystemType.S3.name(), s3FileSystem.getStorageContainer(), deafultFs, locations);
        instanceProfile = s3FileSystem.getInstanceProfile();
    }

    public String getInstanceProfile() {
        return instanceProfile;
    }

    public void setInstanceProfile(String instanceProfile) {
        this.instanceProfile = instanceProfile;
    }

}
