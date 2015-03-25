package com.sequenceiq.cloudbreak.controller.json;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.Set;

import javax.validation.constraints.NotNull;

@ApiModel("HostGroup")
public class HostGroupJson {

    @NotNull
    @ApiModelProperty(required = true)
    private String name;
    @NotNull
    @ApiModelProperty(required = true)
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
