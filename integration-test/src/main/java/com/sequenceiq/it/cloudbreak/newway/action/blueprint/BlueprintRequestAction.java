package com.sequenceiq.it.cloudbreak.newway.action.blueprint;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.blueprint.BlueprintEntity;

public class BlueprintRequestAction implements Action<BlueprintEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintRequestAction.class);

    @Override
    public BlueprintEntity action(TestContext testContext, BlueprintEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getName()));
        logJSON(LOGGER, format(" Blueprint request request:%n"), entity.getName());
        entity.setRequest(
                client.getCloudbreakClient()
                        .blueprintV4Endpoint()
                        .getRequest(client.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, format(" Blueprint requested successfully:%n"), entity.getRequest());

        return entity;
    }

}