package com.sequenceiq.it.cloudbreak.newway.action.v4.recipe;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.recipe.RecipeTestDto;

public class RecipeCreateAction implements Action<RecipeTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeCreateAction.class);

    @Override
    public RecipeTestDto action(TestContext testContext, RecipeTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        logJSON(LOGGER, format(" Recipe post request:%n"), testDto.getRequest());
        testDto.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .recipeV4Endpoint()
                        .post(cloudbreakClient.getWorkspaceId(), testDto.getRequest()));
        logJSON(LOGGER, format(" Recipe created  successfully:%n"), testDto.getResponse());
        log(LOGGER, format(" ID: %s", testDto.getResponse().getId()));

        return testDto;
    }
}