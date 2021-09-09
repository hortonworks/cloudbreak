package com.sequenceiq.cloudbreak.converter.v4.recipes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class RecipeToRecipeV4RequestConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeToRecipeV4RequestConverter.class);

    public RecipeV4Request convert(Recipe source) {
        RecipeV4Request recipeRequest = new RecipeV4Request();
        recipeRequest.setName("");
        recipeRequest.setDescription(source.getDescription());
        recipeRequest.setType(RecipeV4Type.valueOf(source.getRecipeType().name()));
        recipeRequest.setContent(source.getContent());
        return recipeRequest;
    }

}
