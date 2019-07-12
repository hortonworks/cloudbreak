package com.sequenceiq.sdx.api.model;

import com.sequenceiq.common.api.cloudstorage.CloudStorageV1Base;
import com.sequenceiq.common.api.filesystem.FileSystemType;

public class SdxCloudStorageRequest extends CloudStorageV1Base {

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
