package com.sequenceiq.cloudbreak.converter.v4.recipes;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4ViewResponse;
import com.sequenceiq.cloudbreak.converter.CompactViewToCompactViewResponseConverter;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;

@Component
public class RecipeViewToRecipeV4ViewResponseConverter extends CompactViewToCompactViewResponseConverter<RecipeView, RecipeV4ViewResponse> {
    @Override
    public RecipeV4ViewResponse convert(RecipeView recipe) {
        RecipeV4ViewResponse json = super.convert(recipe);
        json.setType(recipe.getRecipeType());
        return json;
    }

    @Override
    protected RecipeV4ViewResponse createTarget() {
        return new RecipeV4ViewResponse();
    }
}
