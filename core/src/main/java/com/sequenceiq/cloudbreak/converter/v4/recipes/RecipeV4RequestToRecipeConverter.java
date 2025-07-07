package com.sequenceiq.cloudbreak.converter.v4.recipes;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.domain.CreationType;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class RecipeV4RequestToRecipeConverter {

    public Recipe convert(RecipeV4Request source, String accountId) {
        Recipe recipe = new Recipe();
        recipe.setName(source.getName());
        recipe.setDescription(source.getDescription());
        recipe.setRecipeType(recipeType(source.getType()));
        recipe.setContent(source.getContent());
        recipe.setCreationType(CreationType.USER);
        recipe.setAccountId(accountId);
        return recipe;
    }

    private RecipeType recipeType(RecipeV4Type recipeType) {
        if (recipeType.equals(RecipeV4Type.POST_AMBARI_START)) {
            return RecipeType.POST_CLOUDERA_MANAGER_START;
        } else if (recipeType.equals(RecipeV4Type.PRE_AMBARI_START)) {
            return RecipeType.PRE_SERVICE_DEPLOYMENT;
        } else if (recipeType.equals(RecipeV4Type.POST_CLUSTER_INSTALL)) {
            return RecipeType.POST_SERVICE_DEPLOYMENT;
        } else if (recipeType.equals(RecipeV4Type.PRE_CLOUDERA_MANAGER_START)) {
            return RecipeType.PRE_SERVICE_DEPLOYMENT;
        }
        return RecipeType.valueOf(recipeType.name());
    }
}
