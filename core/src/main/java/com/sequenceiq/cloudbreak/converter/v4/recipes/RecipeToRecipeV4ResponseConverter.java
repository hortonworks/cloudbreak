package com.sequenceiq.cloudbreak.converter.v4.recipes;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class RecipeToRecipeV4ResponseConverter extends AbstractConversionServiceAwareConverter<Recipe, RecipeV4Response> {

    @Override
    public RecipeV4Response convert(Recipe recipe) {
        RecipeV4Response json = new RecipeV4Response();
        json.setName(recipe.getName());
        json.setDescription(recipe.getDescription());
        json.setType(RecipeV4Type.valueOf(recipe.getRecipeType().name()));
        json.setContent(recipe.getContent());
        json.setId(recipe.getId());
        WorkspaceResourceResponse workspace = getConversionService().convert(recipe.getWorkspace(), WorkspaceResourceResponse.class);
        json.setWorkspace(workspace);
        return json;
    }
}
