package com.sequenceiq.it.cloudbreak.action.v4.environment;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class EnvironmentListAction implements Action<EnvironmentTestDto, CloudbreakClient> {

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        testDto.setResponseSimpleEnvSet(
                cloudbreakClient.getCloudbreakClient()
                        .environmentV4Endpoint()
                        .list(cloudbreakClient.getWorkspaceId()).getResponses());
        Log.logJSON("Environment list response: ", testDto.getResponse());
        return testDto;
    }
}