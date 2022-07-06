package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.common.model.FileSystemType;

public class SpiFileSystem extends DynamicModel {

    private String name;

    private FileSystemType type;

    private List<CloudFileSystemView> cloudFileSystems;

    public SpiFileSystem(String name, FileSystemType type, List<CloudFileSystemView> cloudFileSystems) {
        this(name, type, cloudFileSystems, new HashMap<>());
    }

    @JsonCreator
    public SpiFileSystem(
            @JsonProperty("name") String name,
            @JsonProperty("type") FileSystemType type,
            @JsonProperty("cloudFileSystems") List<CloudFileSystemView> cloudFileSystems,
            @JsonProperty("parameters") Map<String, Object> parameters) {

        super(parameters);

        this.name = name;
        this.type = type;
        this.cloudFileSystems = Objects.requireNonNullElseGet(cloudFileSystems, ArrayList::new);
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
