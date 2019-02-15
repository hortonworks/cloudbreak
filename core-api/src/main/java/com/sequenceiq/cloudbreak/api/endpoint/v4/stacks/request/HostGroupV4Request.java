package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.HostGroupV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostGroupV4Request extends HostGroupV4Base {

    @ApiModelProperty(HostGroupModelDescription.RECIPES)
    private Set<RecipeV4Request> recipes = new HashSet<>();

    @NotNull
    @ApiModelProperty(value = HostGroupModelDescription.HOST_COUNT, required = true)
    private Integer hostCount;

    public Set<RecipeV4Request> getRecipes() {
        return recipes;
    }

    public void setRecipes(Set<RecipeV4Request> recipes) {
        this.recipes = recipes;
    }

    public Integer getHostCount() {
        return hostCount;
    }

    public void setHostCount(Integer hostCount) {
        this.hostCount = hostCount;
    }
}
