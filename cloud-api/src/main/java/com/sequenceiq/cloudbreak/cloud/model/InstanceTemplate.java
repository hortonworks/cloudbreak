package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.List;

public class InstanceTemplate {

    private String flavor;

    private String groupName;

    private long privateId;

    private List<Volume> volumes;

    public InstanceTemplate(String flavor, String groupName, long privateId) {
        this.flavor = flavor;
        this.groupName = groupName;
        this.privateId = privateId;
        volumes = new ArrayList<>();
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

    public long getPrivateId() {
        return privateId;
    }

    public void addVolume(Volume volume) {
        volumes.add(volume);
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "InstanceTemplate{" +
                "flavor='" + flavor + '\'' +
                ", groupName='" + groupName + '\'' +
                ", privateId=" + privateId +
                ", volumes=" + volumes +
                '}';
    }

    //END GENERATED CODE
}
