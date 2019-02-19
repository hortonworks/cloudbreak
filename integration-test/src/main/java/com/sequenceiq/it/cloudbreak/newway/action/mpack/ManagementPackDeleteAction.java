package com.sequenceiq.it.cloudbreak.newway.action.mpack;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ManagementPackEntity;

public class ManagementPackDeleteAction implements Action<ManagementPackEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementPackDeleteAction.class);

    @Override
    public ManagementPackEntity action(TestContext testContext, ManagementPackEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, "ManagementPack name: " + entity.getName());
        log(LOGGER, " ManagementPack delete request");
        entity.setResponse(
                client.getCloudbreakClient()
                        .managementPackV4Endpoint()
                        .deleteInWorkspace(client.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, " ManagementPack deleted successfully:\n", entity.getResponse());
        log(LOGGER, "ManagementPack ID: " + entity.getResponse().getId());

        return entity;
    }
}
