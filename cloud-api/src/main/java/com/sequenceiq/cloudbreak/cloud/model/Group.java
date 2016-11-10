package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;

public class Group {

    private final String name;

    private final InstanceGroupType type;

    private final List<CloudInstance> instances;

    private final Security security;

    private Optional<CloudInstance> skeleton = Optional.empty();

    public Group(String name, InstanceGroupType type, List<CloudInstance> instances, Security security, CloudInstance skeleton) {
        this.name = name;
        this.type = type;
        this.instances = ImmutableList.copyOf(instances);
        this.security = security;
        this.skeleton = Optional.ofNullable(skeleton);
    }

    public CloudInstance getReferenceInstanceConfiguration() {
        if (instances.isEmpty()) {
            return skeleton.get();
        }
        return instances.get(0);
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

    public Integer getInstancesSize() {
        return instances.size();
    }

    public Security getSecurity() {
        return security;
    }
}
