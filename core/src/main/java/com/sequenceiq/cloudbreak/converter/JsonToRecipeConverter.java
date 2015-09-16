package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine.DEFAULT_RECIPE_TIMEOUT;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.RecipeRequest;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class JsonToRecipeConverter extends AbstractConversionServiceAwareConverter<RecipeRequest, Recipe> {



    @Override
    public Recipe convert(RecipeRequest json) {
        Recipe recipe = new Recipe();
        recipe.setName(json.getName());
        recipe.setDescription(json.getDescription());
        recipe.setKeyValues(json.getProperties());
        recipe.setPlugins(json.getPlugins());
        recipe.setTimeout(json.getTimeout() == null ? DEFAULT_RECIPE_TIMEOUT : json.getTimeout());
        return recipe;
    }
}
