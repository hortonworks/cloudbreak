package com.sequenceiq.it.cloudbreak.action.v4.environment;

import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class EnvironmentDeleteAction implements Action<EnvironmentTestDto, CloudbreakClient> {

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        SimpleEnvironmentResponse delete = cloudbreakClient.getCloudbreakClient()
                .environmentV4Endpoint()
                .delete(cloudbreakClient.getWorkspaceId(), testDto.getName());
        Log.logJSON("Environment delete response: ", delete);
        return testDto;
    }
}