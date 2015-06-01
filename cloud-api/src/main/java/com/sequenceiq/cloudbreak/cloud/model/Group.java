package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.domain.InstanceGroupType;

public class Group {

    private String name;

    private InstanceGroupType type;

    private List<Instance> instances;

    public Group(String name, InstanceGroupType type) {
        this.name = name;
        this.type = type;
        instances = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public InstanceGroupType getType() {
        return type;
    }

    public List<Instance> getInstances() {
        return instances;
    }

    public void addInstance(Instance instance) {
        instances.add(instance);
    }

}
