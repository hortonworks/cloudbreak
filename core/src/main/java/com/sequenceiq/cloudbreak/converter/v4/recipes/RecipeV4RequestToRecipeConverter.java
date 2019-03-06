package com.sequenceiq.cloudbreak.converter.v4.recipes;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class RecipeV4RequestToRecipeConverter extends AbstractConversionServiceAwareConverter<RecipeV4Request, Recipe> {

    @Override
    public Recipe convert(RecipeV4Request source) {
        Recipe recipe = new Recipe();
        recipe.setName(source.getName());
        recipe.setDescription(source.getDescription());
        recipe.setRecipeType(RecipeType.valueOf(source.getType().name()));
        recipe.setContent(source.getContent());
        return recipe;
    }
}
