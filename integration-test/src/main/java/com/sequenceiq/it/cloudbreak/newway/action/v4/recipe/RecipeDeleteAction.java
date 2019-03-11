package com.sequenceiq.it.cloudbreak.newway.action.v4.recipe;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.RecipeTestDto;

public class RecipeDeleteAction implements Action<RecipeTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeDeleteAction.class);

    @Override
    public RecipeTestDto action(TestContext testContext, RecipeTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Recipe post request:%n"), entity.getRequest());
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .recipeV4Endpoint()
                        .delete(cloudbreakClient.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, format(" Recipe created  successfully:%n"), entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }
}