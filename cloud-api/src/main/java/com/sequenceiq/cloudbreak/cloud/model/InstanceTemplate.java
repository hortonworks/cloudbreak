package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.List;

public class InstanceTemplate {

    private String flavor;
    private String groupName;
    private Long privateId;
    private List<Volume> volumes;
    private InstanceStatus status;

    public InstanceTemplate(String flavor, String groupName, Long privateId, InstanceStatus status) {
        this.flavor = flavor;
        this.groupName = groupName;
        this.privateId = privateId;
        this.status = status;
        volumes = new ArrayList<>();
    }

    public InstanceTemplate(String flavor, String groupName, Long privateId, List<Volume> volumes, InstanceStatus status) {
        this.flavor = flavor;
        this.groupName = groupName;
        this.privateId = privateId;
        this.volumes = volumes;
        this.status = status;
    }

    public String getFlavor() {
        return flavor;
    }

    public List<Volume> getVolumes() {
        return volumes;
    }

    public String getGroupName() {
        return groupName;
    }

    public Long getPrivateId() {
        return privateId;
    }

    public void addVolume(Volume volume) {
        volumes.add(volume);
    }

    public InstanceStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("InstanceTemplate{");
        sb.append("flavor='").append(flavor).append('\'');
        sb.append(", groupName='").append(groupName).append('\'');
        sb.append(", privateId=").append(privateId);
        sb.append(", volumes=").append(volumes);
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }
}
