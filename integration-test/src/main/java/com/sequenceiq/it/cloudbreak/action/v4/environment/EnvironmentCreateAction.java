package com.sequenceiq.it.cloudbreak.action.v4.environment;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

public class EnvironmentCreateAction implements Action<EnvironmentTestDto> {

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        // TODO: implement this against the environment service
//        testDto.setResponse(
//                cloudbreakClient.getCloudbreakClient()
//                        .environmentV4Endpoint()
//                        .post(cloudbreakClient.getWorkspaceId(), testDto.getRequest()));
//
//        Log.logJSON("Environment post request: ", testDto.getRequest());
        return testDto;
    }
}