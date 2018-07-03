package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class RecipeBuilder {

    public List<Recipe> buildRecipes(String recipeName, List<RecipeScript> recipeScripts) {
        List<Recipe> recipes = new ArrayList<>();
        int index = 0;
        for (RecipeScript script : recipeScripts) {
            Recipe recipe = new Recipe();
            if (recipeScripts.size() > 1) {
                recipe.setName(recipeName + '-' + index);
            } else {
                recipe.setName(recipeName);
            }
            recipe.setContent(Base64.encodeBase64String(script.getScript().getBytes()));
            recipe.setRecipeType(script.getRecipeType());
            index++;
            recipes.add(recipe);
        }
        return recipes;
    }
}
