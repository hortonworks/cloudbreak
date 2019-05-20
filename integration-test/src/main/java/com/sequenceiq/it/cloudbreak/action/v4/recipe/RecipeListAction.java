package com.sequenceiq.it.cloudbreak.action.v4.recipe;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;

public class RecipeListAction implements Action<RecipeTestDto> {

    @Override
    public RecipeTestDto action(TestContext testContext, RecipeTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        testDto.setSimpleResponses(
                cloudbreakClient.getCloudbreakClient().recipeV4Endpoint().list(cloudbreakClient.getWorkspaceId())
        );
        return testDto;
    }
}
