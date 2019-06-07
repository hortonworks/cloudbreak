package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.cloudbreak.common.type.filesystem.FileSystemType;

public class SpiFileSystem extends DynamicModel {

    private String name;

    private FileSystemType type;

    private CloudFileSystemView cloudFileSystem;

    public SpiFileSystem(String name, FileSystemType type, CloudFileSystemView cloudFileSystem) {
        this.name = name;
        this.type = type;
        this.cloudFileSystem = cloudFileSystem;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FileSystemType getType() {
        return type;
    }

    public void setType(FileSystemType type) {
        this.type = type;
    }

    public CloudFileSystemView getCloudFileSystem() {
        return cloudFileSystem;
    }

    public void setCloudFileSystem(CloudFileSystemView cloudFileSystem) {
        this.cloudFileSystem = cloudFileSystem;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FileSystem{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
