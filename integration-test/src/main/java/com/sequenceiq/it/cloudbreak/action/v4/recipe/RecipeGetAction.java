package com.sequenceiq.it.cloudbreak.action.v4.recipe;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class RecipeGetAction implements Action<RecipeTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeGetAction.class);

    @Override
    public RecipeTestDto action(TestContext testContext, RecipeTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        testDto.setResponse(
                cloudbreakClient.getDefaultClient(testContext).recipeV4Endpoint().getByName(cloudbreakClient.getWorkspaceId(), testDto.getName())
        );
        Log.whenJson(LOGGER, format(" Recipe get successfully:%n"), testDto.getResponse());

        return testDto;
    }
}