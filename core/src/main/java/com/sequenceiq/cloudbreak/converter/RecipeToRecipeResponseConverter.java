package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.domain.Recipe;
import org.springframework.stereotype.Component;

@Component
public class RecipeToRecipeResponseConverter extends AbstractConversionServiceAwareConverter<Recipe, RecipeResponse> {
    @Override
    public RecipeResponse convert(Recipe recipe) {
        RecipeResponse json = new RecipeResponse();
        json.setName(recipe.getName());
        json.setDescription(recipe.getDescription());
        json.setRecipeType(recipe.getRecipeType());
        json.setContent(recipe.getContent());
        json.setId(recipe.getId());
        json.setPublicInAccount(recipe.isPublicInAccount());
        json.setUri(recipe.getUri());
        return json;
    }
}
