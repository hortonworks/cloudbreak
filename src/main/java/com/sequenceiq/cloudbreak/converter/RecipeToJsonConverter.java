package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.RecipeJson;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class RecipeToJsonConverter extends AbstractConversionServiceAwareConverter<Recipe, RecipeJson> {
    @Override
    public RecipeJson convert(Recipe recipe) {
        RecipeJson json = new RecipeJson();
        json.setName(recipe.getName());
        json.setDescription(recipe.getDescription());
        json.setProperties(recipe.getKeyValues());
        json.setPlugins(recipe.getPlugins());
        json.setId(recipe.getId().toString());
        json.setTimeout(recipe.getTimeout());
        return json;
    }
}
