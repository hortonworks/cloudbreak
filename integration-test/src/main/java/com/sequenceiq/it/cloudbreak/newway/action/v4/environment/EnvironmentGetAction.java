package com.sequenceiq.it.cloudbreak.newway.action.v4.environment;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.environment.EnvironmentTestDto;

public class EnvironmentGetAction implements Action<EnvironmentTestDto> {

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        testDto.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .environmentV4Endpoint()
                        .get(cloudbreakClient.getWorkspaceId(), testDto.getName()));
        logJSON("Environment get response: ", testDto.getResponse());
        return testDto;
    }
}