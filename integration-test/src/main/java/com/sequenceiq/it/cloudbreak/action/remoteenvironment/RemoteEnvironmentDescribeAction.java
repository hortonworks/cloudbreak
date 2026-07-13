package com.sequenceiq.it.cloudbreak.action.remoteenvironment;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.remoteenvironment.DescribeRemoteEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.RemoteEnvironmentClient;

public class RemoteEnvironmentDescribeAction implements Action<DescribeRemoteEnvironmentTestDto, RemoteEnvironmentClient> {
    @Override
    public DescribeRemoteEnvironmentTestDto action(TestContext testContext, DescribeRemoteEnvironmentTestDto testDto, RemoteEnvironmentClient client)
            throws Exception {
        testDto.setResponse(client.getEndpoint(testContext).remoteEnvironmentEndpoint().getByCrn(testDto.getRequest()));
        Log.whenJson("Remote Environment describe response: ", testDto.getResponse());

        return testDto;
    }
}
