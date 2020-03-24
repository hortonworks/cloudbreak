package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.common.model.FileSystemType;

public class SpiFileSystem extends DynamicModel {

    private String name;

    private FileSystemType type;

    private List<CloudFileSystemView> cloudFileSystems;

    public SpiFileSystem(String name, FileSystemType type, List<CloudFileSystemView> cloudFileSystems) {
        this.name = name;
        this.type = type;
        if (cloudFileSystems != null) {
            this.cloudFileSystems = cloudFileSystems;
        } else {
            this.cloudFileSystems = new ArrayList<>();
        }
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

    public List<CloudFileSystemView> getCloudFileSystems() {
        return cloudFileSystems;
    }

    public void setCloudFileSystems(List<CloudFileSystemView> cloudFileSystems) {
        if (cloudFileSystems != null) {
            this.cloudFileSystems = cloudFileSystems;
        } else {
            this.cloudFileSystems = new ArrayList<>();
        }
    }

    @Override
    public String toString() {
        return "FileSystem{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", cloudFileSystems=" + cloudFileSystems +
                '}';
    }
}
