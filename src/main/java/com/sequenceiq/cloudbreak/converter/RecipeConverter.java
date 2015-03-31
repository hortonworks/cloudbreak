package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.RecipeJson;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class RecipeConverter extends AbstractConverter<RecipeJson, Recipe> {

    public static final int DEFAULT_TIMEOUT = 10;

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

    public Recipe convert(RecipeJson json, boolean publicInAccount) {
        Recipe recipe = convert(json);
        recipe.setPublicInAccount(publicInAccount);
        return recipe;
    }
}
