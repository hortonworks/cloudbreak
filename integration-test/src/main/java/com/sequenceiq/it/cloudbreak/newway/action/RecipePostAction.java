package com.sequenceiq.it.cloudbreak.newway.action;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.RecipeEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

public class RecipePostAction implements Action<RecipeEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipePostAction.class);

    @Override
    public RecipeEntity action(TestContext testContext, RecipeEntity entity, CloudbreakClient client) throws Exception {
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

}