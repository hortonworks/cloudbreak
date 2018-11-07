package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

@Component
public class RecipeToRecipeResponseConverter extends AbstractConversionServiceAwareConverter<Recipe, RecipeResponse> {

    @Inject
    private SecretService secretService;

    @Override
    public RecipeResponse convert(Recipe recipe) {
        RecipeResponse json = new RecipeResponse();
        json.setName(recipe.getName());
        json.setDescription(recipe.getDescription());
        json.setRecipeType(recipe.getRecipeType());
        json.setContent(secretService.get(recipe.getContent()));
        json.setId(recipe.getId());
        json.setUri(recipe.getUri());
        WorkspaceResourceResponse workspace = getConversionService().convert(recipe.getWorkspace(), WorkspaceResourceResponse.class);
        json.setWorkspace(workspace);
        return json;
    }
}
