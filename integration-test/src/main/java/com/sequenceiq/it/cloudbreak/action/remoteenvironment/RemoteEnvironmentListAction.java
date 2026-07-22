package com.sequenceiq.it.cloudbreak.action.remoteenvironment;

import java.util.HashSet;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.remoteenvironment.ListRemoteEnvironmentsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.RemoteEnvironmentClient;

public class RemoteEnvironmentListAction implements Action<ListRemoteEnvironmentsTestDto, RemoteEnvironmentClient> {
    @Override
    public ListRemoteEnvironmentsTestDto action(TestContext testContext, ListRemoteEnvironmentsTestDto testDto, RemoteEnvironmentClient client)
            throws Exception {
        testDto.setResponses(new HashSet<>(client.getDefaultClient(testContext).remoteEnvironmentEndpoint().list(null).getResponses()));
        Log.whenJson("Remote Environment list response: ", testDto.getResponses());

        return testDto;
    }
}
