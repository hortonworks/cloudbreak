package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RecipeViewResponse;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;

@Component
public class RecipeViewToRecipeViewResponseConverter extends AbstractConversionServiceAwareConverter<RecipeView, RecipeViewResponse> {
    @Override
    public RecipeViewResponse convert(RecipeView recipe) {
        RecipeViewResponse json = new RecipeViewResponse();
        json.setName(recipe.getName());
        json.setDescription(recipe.getDescription());
        json.setRecipeType(recipe.getRecipeType());
        json.setId(recipe.getId());
        return json;
    }
}
