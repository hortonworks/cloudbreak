package com.sequenceiq.it.cloudbreak.newway.action.v4.mpack;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.mpack.MPackTestDto;

public class MpackCreateAction implements Action<MPackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MpackCreateAction.class);

    @Override
    public MPackTestDto action(TestContext testContext, MPackTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        log(LOGGER, "ManagementPack name: " + entity.getName());
        logJSON(LOGGER, " ManagementPack post request:\n", entity.getRequest());
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .managementPackV4Endpoint()
                        .createInWorkspace(cloudbreakClient.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, " ManagementPack created  successfully:\n", entity.getResponse());
        log(LOGGER, "ManagementPack ID: " + entity.getResponse().getId());

        return entity;
    }
}
