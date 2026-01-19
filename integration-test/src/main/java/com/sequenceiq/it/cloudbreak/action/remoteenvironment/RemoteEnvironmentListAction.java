package com.sequenceiq.it.cloudbreak.action.remoteenvironment;

import java.util.HashSet;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.remoteenvironment.RemoteEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.RemoteEnvironmentClient;

public class RemoteEnvironmentListAction implements Action<RemoteEnvironmentTestDto, RemoteEnvironmentClient> {
    @Override
    public RemoteEnvironmentTestDto action(TestContext testContext, RemoteEnvironmentTestDto testDto, RemoteEnvironmentClient client) throws Exception {
        testDto.setResponses(new HashSet<>(client.getEndpoint(testContext).remoteEnvironmentEndpoint().list(null).getResponses()));
        Log.whenJson("Remote Environment list response: ", testDto.getResponses());

        return testDto;
    }
}
