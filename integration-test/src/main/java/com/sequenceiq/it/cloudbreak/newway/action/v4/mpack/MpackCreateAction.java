package com.sequenceiq.it.cloudbreak.newway.action.v4.mpack;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.mpack.MPackTestDto;

public class MpackCreateAction implements Action<MPackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MpackCreateAction.class);

    @Override
    public MPackTestDto action(TestContext testContext, MPackTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        log(LOGGER, "ManagementPack name: " + testDto.getName());
        logJSON(LOGGER, " ManagementPack post request:\n", testDto.getRequest());
        testDto.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .managementPackV4Endpoint()
                        .createInWorkspace(cloudbreakClient.getWorkspaceId(), testDto.getRequest()));
        logJSON(LOGGER, " ManagementPack created  successfully:\n", testDto.getResponse());
        log(LOGGER, "ManagementPack ID: " + testDto.getResponse().getId());

        return testDto;
    }
}
