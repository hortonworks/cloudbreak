package com.sequenceiq.cloudbreak.converter.v4.recipes;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Response;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;

@Component
public class RecipeViewToRecipeV4ViewResponseConverter {

    public RecipeViewV4Response convert(RecipeView recipe) {
        RecipeViewV4Response json = new RecipeViewV4Response();
        json.setName(recipe.getName());
        json.setDescription(recipe.getDescription());
        json.setId(recipe.getId());
        json.setType(recipe.getRecipeType());
        json.setCrn(recipe.getResourceCrn());
        json.setCreated(recipe.getCreated());
        return json;
    }
}
