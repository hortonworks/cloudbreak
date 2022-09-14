package com.sequenceiq.cloudbreak.service.cost.model;

import java.util.List;

public class Cluster {

    private String region;

    private List<Instance> instances;

    public String getRegion() {
        return region;
    }

    public List<Instance> getInstances() {
        return instances;
    }
}
