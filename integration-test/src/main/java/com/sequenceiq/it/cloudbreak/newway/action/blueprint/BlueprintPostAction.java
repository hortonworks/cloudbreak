package com.sequenceiq.it.cloudbreak.newway.action.blueprint;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.entity.blueprint.BlueprintEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class BlueprintPostAction implements Action<BlueprintEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintPostAction.class);

    @Override
    public BlueprintEntity action(TestContext testContext, BlueprintEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Blueprint post request:%n"), entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .blueprintV4Endpoint()
                        .post(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, format(" Blueprint created  successfully:%n"), entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }

}