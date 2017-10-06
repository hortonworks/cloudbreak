package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("HostGroupRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostGroupRequest extends HostGroupBase {

    @ApiModelProperty(HostGroupModelDescription.RECIPES)
    private Set<RecipeRequest> recipes = new HashSet<>();

    @ApiModelProperty(HostGroupModelDescription.RECIPE_NAMES)
    private Set<String> recipeNames = new HashSet<>();

    public Set<RecipeRequest> getRecipes() {
        return recipes;
    }

    public void setRecipes(Set<RecipeRequest> recipes) {
        this.recipes = recipes;
    }

    public Set<String> getRecipeNames() {
        return recipeNames;
    }

    public void setRecipeNames(Set<String> recipeNames) {
        this.recipeNames = recipeNames;
    }
}
