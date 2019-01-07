package com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.api.model.CompactViewResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@ApiModel
public class RecipeV4ViewResponses {

    private Set<RecipeV4ViewResponse> recipes = new HashSet<>();

    public Set<RecipeV4ViewResponse> getRecipes() {
        return recipes;
    }

    public void setRecipes(Set<RecipeV4ViewResponse> recipes) {
        this.recipes = recipes;
    }

    public static final RecipeV4ViewResponses recipeV4ViewResponses(Set<RecipeV4ViewResponse> recipes) {
        RecipeV4ViewResponses recipeV4ViewResponses = new RecipeV4ViewResponses();
        recipeV4ViewResponses.setRecipes(recipes);
        return recipeV4ViewResponses;
    }
}
