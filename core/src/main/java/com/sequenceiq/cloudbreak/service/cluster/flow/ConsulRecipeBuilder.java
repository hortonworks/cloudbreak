package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.RecipeType;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class ConsulRecipeBuilder implements RecipeBuilder {

    @Override
    public List<Recipe> buildRecipes(String recipeName, List<RecipeScript> recipeScripts) {
        List<Recipe> recipes = new ArrayList<>();
        int index = 0;
        for (RecipeScript script : recipeScripts) {
            Recipe recipe = new Recipe();
            if (recipeScripts.size() > 1) {
                recipe.setName(recipeName + "-" + index);
            } else {
                recipe.setName(recipeName);
            }
            recipe.setContent(Base64.encodeBase64String(script.getScript().getBytes()));
            switch (script.getClusterLifecycleEvent()) {
                case PRE_INSTALL:
                    recipe.setRecipeType(RecipeType.PRE);
                    break;
                case POST_INSTALL:
                    recipe.setRecipeType(RecipeType.POST);
                    break;
                default:
                    throw new UnsupportedOperationException("Cluster lifecycle event " + script.getClusterLifecycleEvent() + " is not supported");
            }
            index++;
            recipes.add(recipe);
        }

        return recipes;
    }
}
