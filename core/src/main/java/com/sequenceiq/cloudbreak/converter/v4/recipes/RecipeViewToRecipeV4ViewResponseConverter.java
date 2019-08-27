package com.sequenceiq.cloudbreak.converter.v4.recipes;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Response;
import com.sequenceiq.cloudbreak.converter.CompactViewToCompactViewResponseConverter;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;

@Component
public class RecipeViewToRecipeV4ViewResponseConverter extends CompactViewToCompactViewResponseConverter<RecipeView, RecipeViewV4Response> {
    @Override
    public RecipeViewV4Response convert(RecipeView recipe) {
        RecipeViewV4Response json = super.convert(recipe);
        json.setType(recipe.getRecipeType());
        json.setCrn(recipe.getResourceCrn());
        json.setCreated(recipe.getCreated());
        return json;
    }

    @Override
    protected RecipeViewV4Response createTarget() {
        return new RecipeViewV4Response();
    }
}
