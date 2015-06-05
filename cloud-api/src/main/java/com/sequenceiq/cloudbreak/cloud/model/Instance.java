package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.List;

public class Instance {

    private String flavor;

    private String groupName;

    private int privateId;

    private InstanceMetaData metaData;

    private List<Volume> volumes;

    public Instance(String flavor, String groupName, int privateId) {
        this.flavor = flavor;
        this.groupName = groupName;
        this.privateId = privateId;
        volumes = new ArrayList<>();
    }

    public String getFlavor() {
        return flavor;
    }

    public InstanceMetaData getMetaData() {
        return metaData;
    }

    public List<Volume> getVolumes() {
        return volumes;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getPrivateId() {
        return privateId;
    }

    public void addVolume(Volume volume) {
        volumes.add(volume);
    }

    public void addMetaData(InstanceMetaData metaData) {
        this.metaData = metaData;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "Instance{" +
                "flavor='" + flavor + '\'' +
                ", metaData=" + metaData +
                ", volumes=" + volumes +
                '}';
    }
    //END GENERATED CODE
}
