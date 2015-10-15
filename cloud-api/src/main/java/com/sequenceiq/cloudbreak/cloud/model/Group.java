package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;

public class Group {

    private final String name;
    private final InstanceGroupType type;
    private final List<InstanceTemplate> instances;

    public Group(String name, InstanceGroupType type, List<InstanceTemplate> instances) {
        this.name = name;
        this.type = type;
        this.instances = ImmutableList.copyOf(instances);
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

}
