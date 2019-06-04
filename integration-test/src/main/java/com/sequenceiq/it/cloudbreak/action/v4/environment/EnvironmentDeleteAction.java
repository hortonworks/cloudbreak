package com.sequenceiq.it.cloudbreak.action.v4.environment;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class EnvironmentDeleteAction implements Action<EnvironmentTestDto, CloudbreakClient> {

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        SimpleEnvironmentV4Response delete = cloudbreakClient.getCloudbreakClient()
                .environmentV4Endpoint()
                .delete(cloudbreakClient.getWorkspaceId(), testDto.getName());
        Log.logJSON("Environment delete response: ", delete);
        return testDto;
    }
}