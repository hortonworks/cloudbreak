package com.sequenceiq.cloudbreak.controller.json;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

public class StackValidationRequest implements JsonEntity {
    private Set<HostGroupJson> hostGroups = new HashSet<>();
    private Set<InstanceGroupJson> instanceGroups = new HashSet<>();
    @NotNull
    private Long blueprintId;

    public Set<HostGroupJson> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<HostGroupJson> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public Set<InstanceGroupJson> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroupJson> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }
}
