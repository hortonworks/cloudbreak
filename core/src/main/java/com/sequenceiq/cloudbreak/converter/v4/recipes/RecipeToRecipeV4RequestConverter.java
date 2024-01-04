package com.sequenceiq.cloudbreak.converter.v4.recipes;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class RecipeToRecipeV4RequestConverter {

    @Inject
    private RecipeTypeToRecipeV4TypeConverter recipeTypeToRecipeV4TypeConverter;

    public RecipeV4Request convert(Recipe source) {
        RecipeV4Request recipeRequest = new RecipeV4Request();
        recipeRequest.setName(source.getName());
        recipeRequest.setDescription(source.getDescription());
        recipeRequest.setType(recipeTypeToRecipeV4TypeConverter.convert(source.getRecipeType()));
        recipeRequest.setContent(source.getContent());
        return recipeRequest;
    }

}
