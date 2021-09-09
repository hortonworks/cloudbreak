package com.sequenceiq.cloudbreak.converter.v4.recipes;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.converter.v4.workspaces.WorkspaceToWorkspaceResourceV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class RecipeToRecipeV4ResponseConverter {

    @Inject
    private WorkspaceToWorkspaceResourceV4ResponseConverter workspaceToWorkspaceResourceV4ResponseConverter;

    public RecipeV4Response convert(Recipe recipe) {
        RecipeV4Response json = new RecipeV4Response();
        json.setName(recipe.getName());
        json.setDescription(recipe.getDescription());
        json.setType(RecipeV4Type.valueOf(recipe.getRecipeType().name()));
        json.setContent(recipe.getContent());
        WorkspaceResourceV4Response workspace =  workspaceToWorkspaceResourceV4ResponseConverter
                .convert(recipe.getWorkspace());
        json.setWorkspace(workspace);
        json.setCreator(recipe.getCreator());
        json.setCrn(recipe.getResourceCrn());
        json.setCreated(recipe.getCreated());
        return json;
    }

}
