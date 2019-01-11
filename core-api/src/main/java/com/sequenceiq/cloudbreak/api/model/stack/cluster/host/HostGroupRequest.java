package com.sequenceiq.cloudbreak.api.model.stack.cluster.host;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostGroupRequest extends HostGroupBase {

    @ApiModelProperty(HostGroupModelDescription.RECIPES)
    private Set<RecipeV4Request> recipes = new HashSet<>();

    @ApiModelProperty(HostGroupModelDescription.RECIPE_NAMES)
    private Set<String> recipeNames = new HashSet<>();

    public Set<RecipeV4Request> getRecipes() {
        return recipes;
    }

    public void setRecipes(Set<RecipeV4Request> recipes) {
        this.recipes = recipes;
    }

    public Set<String> getRecipeNames() {
        return recipeNames;
    }

    public void setRecipeNames(Set<String> recipeNames) {
        this.recipeNames = recipeNames;
    }
}
