package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RecipeViewResponse;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;

@Component
public class RecipeViewToRecipeViewResponseConverter extends CompactViewToCompactViewResponseConverter<RecipeView, RecipeViewResponse> {
    @Override
    public RecipeViewResponse convert(RecipeView recipe) {
        RecipeViewResponse json = super.convert(recipe);
        json.setRecipeType(recipe.getRecipeType());
        return json;
    }

    @Override
    protected RecipeViewResponse createTarget() {
        return new RecipeViewResponse();
    }
}
