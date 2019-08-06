package com.sequenceiq.cloudbreak.template.filesystem.s3;

import java.util.Collection;

import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.api.filesystem.S3FileSystem;

public class S3FileSystemConfigurationsView extends BaseFileSystemConfigurationsView {

    private String instanceProfile;

    private String s3GuardDynamoTableName;

    public S3FileSystemConfigurationsView(S3FileSystem s3FileSystem, Collection<StorageLocationView> locations, boolean deafultFs) {
        super(FileSystemType.S3.name(), s3FileSystem.getStorageContainer(), deafultFs, locations);
        instanceProfile = s3FileSystem.getInstanceProfile();
        s3GuardDynamoTableName = s3FileSystem.getS3GuardDynamoTableName();
    }

    public String getInstanceProfile() {
        return instanceProfile;
    }

    public void setInstanceProfile(String instanceProfile) {
        this.instanceProfile = instanceProfile;
    }

    public String getS3GuardDynamoTableName() {
        return s3GuardDynamoTableName;
    }

    public void setS3GuardDynamoTableName(String s3GuardDynamoTableName) {
        this.s3GuardDynamoTableName = s3GuardDynamoTableName;
    }
}
