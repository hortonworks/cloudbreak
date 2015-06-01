package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.List;

public class Instance {

    private String flavor;

    private List<Volume> volumes;

    public Instance(String flavor) {
        this.flavor = flavor;
        volumes = new ArrayList<>();
    }

    public String getFlavor() {
        return flavor;
    }

    public List<Volume> getVolumes() {
        return volumes;
    }

    public void addVolume(Volume volume) {
        volumes.add(volume);
    }

}
