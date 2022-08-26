package com.sequenceiq.freeipa.api.v1.recipe.model;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.freeipa.api.v1.recipe.doc.RecipeDescription;

import io.swagger.annotations.ApiModelProperty;

public class RecipeAttachDetachRequest {

    @NotNull
    @ApiModelProperty(value = RecipeDescription.ENVIRONMENT, required = true)
    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    private String environmentCrn;

    @NotNull
    @ApiModelProperty(value = RecipeDescription.RECIPE, required = true)
    private List<String> recipes;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public List<String> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<String> recipes) {
        this.recipes = recipes;
    }

    @Override
    public String toString() {
        return "RecipeAttachDetachRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", recipes=" + recipes +
                '}';
    }

}
