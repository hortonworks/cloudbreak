package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.controller.json.RecipeRequest;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class JsonToRecipeConverter extends AbstractConversionServiceAwareConverter<RecipeRequest, Recipe> {

    public static final int DEFAULT_TIMEOUT = 10;

    @Override
    public Recipe convert(RecipeRequest json) {
        Recipe recipe = new Recipe();
        recipe.setName(json.getName());
        recipe.setDescription(json.getDescription());
        recipe.setKeyValues(json.getProperties());
        recipe.setPlugins(json.getPlugins());
        recipe.setTimeout(json.getTimeout() == null ? DEFAULT_TIMEOUT : json.getTimeout());
        return recipe;
    }
}
