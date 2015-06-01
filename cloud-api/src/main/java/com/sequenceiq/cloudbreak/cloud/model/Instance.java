package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.List;

public class Instance {

    private String flavor;

    private InstanceMetaData metaData;

    private List<Volume> volumes;

    public Instance(String flavor) {
        this(flavor, null);
    }

    public Instance(String flavor, InstanceMetaData metaData) {
        this.metaData = metaData;
        this.flavor = flavor;
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

    public void addVolume(Volume volume) {
        volumes.add(volume);
    }

    @Override
    public String toString() {
        return "Instance{" +
                "flavor='" + flavor + '\'' +
                ", metaData=" + metaData +
                ", volumes=" + volumes +
                '}';
    }
}
