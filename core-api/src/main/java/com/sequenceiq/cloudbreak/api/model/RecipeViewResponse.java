package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RecipeModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class RecipeViewResponse extends CompactViewResponse {
    @NotNull
    @ApiModelProperty(RecipeModelDescription.TYPE)
    private RecipeType recipeType;

    public RecipeType getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(RecipeType recipeType) {
        this.recipeType = recipeType;
    }
}
