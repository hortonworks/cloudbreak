package com.sequenceiq.sdx.api.model;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CloudStorageV4Base;
import com.sequenceiq.cloudbreak.common.type.filesystem.FileSystemType;

public class SdxCloudStorageRequest extends CloudStorageV4Base {

    private FileSystemType fileSystemType;

    private String baseLocation;

    public FileSystemType getFileSystemType() {
        return fileSystemType;
    }

    public void setFileSystemType(FileSystemType fileSystemType) {
        this.fileSystemType = fileSystemType;
    }

    public String getBaseLocation() {
        return baseLocation;
    }

    public void setBaseLocation(String baseLocation) {
        this.baseLocation = baseLocation;
    }
}
