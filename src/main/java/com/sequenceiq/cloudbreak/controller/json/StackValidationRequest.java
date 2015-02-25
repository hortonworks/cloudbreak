package com.sequenceiq.cloudbreak.controller.json;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

public class StackValidationRequest implements JsonEntity {
    private List<InstanceGroupJson> instanceGroups = new ArrayList<>();
    @NotNull
    private Long blueprintId;

    public List<InstanceGroupJson> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupJson> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }
}
