package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ManagementPackEntity;

public class ManagementPackCreateAction implements ActionV2<ManagementPackEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementPackCreateAction.class);

    @Override
    public ManagementPackEntity action(TestContext testContext, ManagementPackEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, "ManagementPack name: " + entity.getName());
        logJSON(LOGGER, " ManagementPack post request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .managementPackV4Endpoint()
                        .createInWorkspace(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, " ManagementPack created  successfully:\n", entity.getResponse());
        log(LOGGER, "ManagementPack ID: " + entity.getResponse().getId());

        return entity;
    }
}
