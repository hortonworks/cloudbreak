package com.sequenceiq.it.cloudbreak.action.v4.recipe;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class RecipeDeleteAction implements Action<RecipeTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeDeleteAction.class);

    @Override
    public RecipeTestDto action(TestContext testContext, RecipeTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Log.log(LOGGER, String.format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, format(" Recipe post request:%n"), testDto.getRequest());
        testDto.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .recipeV4Endpoint()
                        .delete(cloudbreakClient.getWorkspaceId(), testDto.getName()));
        Log.logJSON(LOGGER, format(" Recipe created  successfully:%n"), testDto.getResponse());
        Log.log(LOGGER, String.format(" ID: %s", testDto.getResponse().getId()));

        return testDto;
    }
}