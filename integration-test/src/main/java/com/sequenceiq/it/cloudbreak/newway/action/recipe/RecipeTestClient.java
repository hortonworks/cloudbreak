package com.sequenceiq.it.cloudbreak.newway.action.recipe;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.RecipeTestDto;

public class RecipeTestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeTestClient.class);

    private RecipeTestClient() {
    }

    public static RecipeTestDto getByName(TestContext testContext, RecipeTestDto entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().recipeV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static RecipeTestDto postV4(TestContext testContext, RecipeTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Recipe post request:%n"), entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .recipeV4Endpoint()
                        .post(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, format(" Recipe created  successfully:%n"), entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }

    public static RecipeTestDto deleteV4(TestContext testContext, RecipeTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Recipe post request:%n"), entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .recipeV4Endpoint()
                        .delete(client.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, format(" Recipe created  successfully:%n"), entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }
}