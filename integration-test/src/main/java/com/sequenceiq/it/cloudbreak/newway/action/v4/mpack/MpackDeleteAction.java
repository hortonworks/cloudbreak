package com.sequenceiq.it.cloudbreak.newway.action.v4.mpack;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.mpack.MPackTestDto;

public class MpackDeleteAction implements Action<MPackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MpackDeleteAction.class);

    @Override
    public MPackTestDto action(TestContext testContext, MPackTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        log(LOGGER, "ManagementPack name: " + testDto.getName());
        log(LOGGER, " ManagementPack delete request");
        testDto.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .managementPackV4Endpoint()
                        .deleteInWorkspace(cloudbreakClient.getWorkspaceId(), testDto.getName()));
        logJSON(LOGGER, " ManagementPack deleted successfully:\n", testDto.getResponse());
        log(LOGGER, "ManagementPack ID: " + testDto.getResponse().getId());

        return testDto;    }
}
