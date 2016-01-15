package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;

public class Group {

    private final String name;
    private final InstanceGroupType type;
    private final List<CloudInstance> instances;

    public Group(String name, InstanceGroupType type, List<CloudInstance> instances) {
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

    public List<CloudInstance> getInstances() {
        return instances;
    }

}
