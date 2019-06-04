package com.sequenceiq.it.cloudbreak.action.v4.mpack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.mpack.MPackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class MpackDeleteAction implements Action<MPackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MpackDeleteAction.class);

    @Override
    public MPackTestDto action(TestContext testContext, MPackTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Log.log(LOGGER, "ManagementPack name: " + testDto.getName());
        Log.log(LOGGER, " ManagementPack delete request");
        testDto.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .managementPackV4Endpoint()
                        .deleteInWorkspace(cloudbreakClient.getWorkspaceId(), testDto.getName()));
        Log.logJSON(LOGGER, " ManagementPack deleted successfully:\n", testDto.getResponse());
        Log.log(LOGGER, "ManagementPack ID: " + testDto.getResponse().getId());

        return testDto;    }
}
