package com.sequenceiq.cloudbreak.controller.json;

import java.util.Set;

import javax.validation.constraints.NotNull;

public class HostGroupJson {

    @NotNull
    private String name;
    @NotNull
    private String instanceGroupName;
    private Set<Long> recipeIds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstanceGroupName() {
        return instanceGroupName;
    }

    public void setInstanceGroupName(String instanceGroupName) {
        this.instanceGroupName = instanceGroupName;
    }

    public Set<Long> getRecipeIds() {
        return recipeIds;
    }

    public void setRecipeIds(Set<Long> recipeIds) {
        this.recipeIds = recipeIds;
    }
}
