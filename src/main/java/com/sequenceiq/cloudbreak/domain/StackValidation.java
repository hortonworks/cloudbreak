package com.sequenceiq.cloudbreak.domain;

import java.util.Set;

public class StackValidation implements ProvisionEntity {
    private Set<InstanceGroup> instanceGroups;
    private Blueprint blueprint;

    public Set<InstanceGroup> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroup> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public Blueprint getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(Blueprint blueprint) {
        this.blueprint = blueprint;
    }
}
