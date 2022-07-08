package com.sequenceiq.cloudbreak.converter.v4.recipes;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;

@Component
public class RecipeTypeToRecipeV4TypeConverter {

    public RecipeV4Type convert(RecipeType recipeType) {
        switch (recipeType) {
            case PRE_SERVICE_DEPLOYMENT:
                return RecipeV4Type.PRE_CLOUDERA_MANAGER_START;
            case POST_SERVICE_DEPLOYMENT:
                return RecipeV4Type.POST_CLUSTER_INSTALL;
            default:
                return RecipeV4Type.valueOf(recipeType.name());
        }
    }

}
