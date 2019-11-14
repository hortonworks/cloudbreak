package com.sequenceiq.cloudbreak.template.filesystem.s3;

import java.util.Collection;

import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.model.FileSystemType;

public class S3FileSystemConfigurationsView extends BaseFileSystemConfigurationsView {

    private String s3GuardDynamoTableName;

    public S3FileSystemConfigurationsView(S3FileSystem s3FileSystem, Collection<StorageLocationView> locations, boolean defaultFs) {
        super(FileSystemType.S3.name(), s3FileSystem.getStorageContainer(), defaultFs, locations, null);
        s3GuardDynamoTableName = s3FileSystem.getS3GuardDynamoTableName();
    }

    public String getS3GuardDynamoTableName() {
        return s3GuardDynamoTableName;
    }

    public void setS3GuardDynamoTableName(String s3GuardDynamoTableName) {
        this.s3GuardDynamoTableName = s3GuardDynamoTableName;
    }

    @Override
    public String getProtocol() {
        return FileSystemType.S3.getProtocol();
    }
}
