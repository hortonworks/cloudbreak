package com.sequenceiq.it.cloudbreak.newway.action.v4.recipe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.RecipeTestDto;

public class RecipeGetAction implements Action<RecipeTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeGetAction.class);

    @Override
    public RecipeTestDto action(TestContext testContext, RecipeTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().recipeV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }
}