package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.RecipeJson;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class RecipeConverter extends AbstractConverter<RecipeJson, Recipe> {

    @Override
    public RecipeJson convert(Recipe recipe) {
        RecipeJson json = new RecipeJson();
        json.setName(recipe.getName());
        json.setDescription(recipe.getDescription());
        json.setProperties(recipe.getKeyValues());
        json.setPlugins(recipe.getPlugins());
        json.setId(recipe.getId().toString());
        return json;
    }

    @Override
    public Recipe convert(RecipeJson json) {
        Recipe recipe = new Recipe();
        recipe.setName(json.getName());
        recipe.setDescription(json.getDescription());
        recipe.setKeyValues(json.getProperties());
        recipe.setPlugins(json.getPlugins());
        return recipe;
    }

    public Recipe convert(RecipeJson json, boolean publicInAccount) {
        Recipe recipe = convert(json);
        recipe.setPublicInAccount(publicInAccount);
        return recipe;
    }
}
