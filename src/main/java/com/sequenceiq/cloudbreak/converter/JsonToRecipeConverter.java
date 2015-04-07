package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.RecipeJson;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class JsonToRecipeConverter extends AbstractConversionServiceAwareConverter<RecipeJson, Recipe> {

    public static final int DEFAULT_TIMEOUT = 10;

    @Override
    public Recipe convert(RecipeJson json) {
        Recipe recipe = new Recipe();
        recipe.setName(json.getName());
        recipe.setDescription(json.getDescription());
        recipe.setKeyValues(json.getProperties());
        recipe.setPlugins(json.getPlugins());
        recipe.setTimeout(json.getTimeout() == null ? DEFAULT_TIMEOUT : json.getTimeout());
        return recipe;
    }
}
