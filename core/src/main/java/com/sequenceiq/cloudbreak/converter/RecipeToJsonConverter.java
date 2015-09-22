package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.RecipeResponse;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class RecipeToJsonConverter extends AbstractConversionServiceAwareConverter<Recipe, RecipeResponse> {
    @Override
    public RecipeResponse convert(Recipe recipe) {
        RecipeResponse json = new RecipeResponse();
        json.setName(recipe.getName());
        json.setDescription(recipe.getDescription());
        json.setProperties(recipe.getKeyValues());
        json.setPlugins(recipe.getPlugins());
        json.setId(recipe.getId());
        json.setTimeout(recipe.getTimeout());
        json.setPublicInAccount(recipe.isPublicInAccount());
        return json;
    }
}
