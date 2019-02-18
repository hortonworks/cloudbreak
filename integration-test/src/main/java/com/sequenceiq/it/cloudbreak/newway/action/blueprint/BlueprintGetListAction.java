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

public class BlueprintGetListAction implements Action<BlueprintEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintGetListAction.class);

    @Override
    public BlueprintEntity action(TestContext testContext, BlueprintEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Blueprint list by workspace request:%n"), entity.getRequest());
        entity.setViewResponses(
                client.getCloudbreakClient()
                        .blueprintV4Endpoint()
                        .list(client.getWorkspaceId()).getResponses());
        logJSON(LOGGER, format(" Blueprint list has executed successfully:%n"), entity.getViewResponses());

        return entity;
    }

}