package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.UpdateHostGroupRecipes;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateRecipesV4Response implements JsonEntity {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<UpdateHostGroupRecipes> recipesAttached = new ArrayList<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<UpdateHostGroupRecipes> recipesDetached = new ArrayList<>();

    public List<UpdateHostGroupRecipes> getRecipesAttached() {
        return recipesAttached;
    }

    public void setRecipesAttached(List<UpdateHostGroupRecipes> recipesAttached) {
        this.recipesAttached = recipesAttached;
    }

    public List<UpdateHostGroupRecipes> getRecipesDetached() {
        return recipesDetached;
    }

    public void setRecipesDetached(List<UpdateHostGroupRecipes> recipesDetached) {
        this.recipesDetached = recipesDetached;
    }

    @Override
    public String toString() {
        return "UpdateRecipesV4Response{" +
                ", recipesAttached=" + recipesAttached +
                ", recipesDetached=" + recipesDetached +
                '}';
    }
}
