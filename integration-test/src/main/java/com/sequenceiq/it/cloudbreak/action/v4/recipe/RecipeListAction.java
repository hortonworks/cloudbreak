package com.sequenceiq.it.cloudbreak.action.v4.recipe;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class RecipeListAction implements Action<RecipeTestDto, CloudbreakClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeListAction.class);

    @Override
    public RecipeTestDto action(TestContext testContext, RecipeTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        testDto.setSimpleResponses(
                cloudbreakClient.getDefaultClient(testContext).recipeV4Endpoint().list(cloudbreakClient.getWorkspaceId())
        );
        Log.whenJson(LOGGER, format(" Recipe list successfully:%n"), testDto.getSimpleResponses());

        return testDto;
    }
}
