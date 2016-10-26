package com.sequenceiq.cloudbreak.converter;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.domain.Plugin;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class JsonToRecipeConverter extends AbstractConversionServiceAwareConverter<RecipeRequest, Recipe> {
    @Override
    public Recipe convert(RecipeRequest json) {
        Recipe recipe = new Recipe();
        recipe.setName(json.getName());
        recipe.setDescription(json.getDescription());
        recipe.setKeyValues(json.getProperties());
        recipe.setPlugins(json.getPlugins().stream().map(Plugin::new).collect(Collectors.toSet()));
        return recipe;
    }
}
