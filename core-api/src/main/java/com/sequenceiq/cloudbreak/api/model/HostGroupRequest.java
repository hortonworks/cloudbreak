package com.sequenceiq.cloudbreak.api.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("HostGroupRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostGroupRequest extends HostGroupBase {

    @ApiModelProperty(HostGroupModelDescription.RECIPES)
    private Set<RecipeRequest> recipes;

    public Set<RecipeRequest> getRecipes() {
        return recipes;
    }

    public void setRecipes(Set<RecipeRequest> recipes) {
        this.recipes = recipes;
    }
}
