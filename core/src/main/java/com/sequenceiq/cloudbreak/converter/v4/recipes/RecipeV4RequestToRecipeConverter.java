package com.sequenceiq.cloudbreak.converter.v4.recipes;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.domain.CreationType;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class RecipeV4RequestToRecipeConverter {

    public Recipe convert(RecipeV4Request source) {
        Recipe recipe = new Recipe();
        recipe.setName(source.getName());
        recipe.setDescription(source.getDescription());
        recipe.setRecipeType(recipeType(source.getType()));
        recipe.setContent(source.getContent());
        recipe.setCreationType(CreationType.USER);
        return recipe;
    }

    private RecipeType recipeType(RecipeV4Type recipeType) {
        if (recipeType.equals(RecipeV4Type.POST_AMBARI_START)) {
            return RecipeType.POST_CLOUDERA_MANAGER_START;
        } else if (recipeType.equals(RecipeV4Type.PRE_AMBARI_START)) {
            return RecipeType.PRE_CLOUDERA_MANAGER_START;
        }
        return RecipeType.valueOf(recipeType.name());
    }
}
