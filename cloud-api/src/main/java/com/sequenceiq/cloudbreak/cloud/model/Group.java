package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.domain.InstanceGroupType;

public class Group {

    private String name;

    private InstanceGroupType type;

    private List<InstanceTemplate> instances;

    public Group(String name, InstanceGroupType type) {
        this.name = name;
        this.type = type;
        instances = new ArrayList<>();
    }

    public Group(String name, InstanceGroupType type, List<InstanceTemplate> instances) {
        this.name = name;
        this.type = type;
        this.instances = instances;
    }

    public String getName() {
        return name;
    }

    public InstanceGroupType getType() {
        return type;
    }

    public List<InstanceTemplate> getInstances() {
        return instances;
    }

    public void addInstance(InstanceTemplate instance) {
        instances.add(instance);
    }

}
