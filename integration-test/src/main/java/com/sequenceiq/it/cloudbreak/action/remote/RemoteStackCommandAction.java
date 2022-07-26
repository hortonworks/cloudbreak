package com.sequenceiq.it.cloudbreak.action.remote;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackRemoteTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class RemoteStackCommandAction implements Action<StackRemoteTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteStackCommandAction.class);

    public StackRemoteTestDto action(TestContext testContext, StackRemoteTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, format(" Remote command [%s] is going to be executed at resource: '%s'... ", testDto.getRequest().getCommand(),
                testDto.getResourceCrn()));
        Log.whenJson(LOGGER, format(" Stack remote command POST request:%n"), testDto.getRequest());
        testDto.setResponse(
                client.getDefaultClient()
                        .utilV4Endpoint()
                        .remoteCommandExecution(testDto.getResourceCrn(), testDto.getRequest()));
        Log.whenJson(LOGGER, format(" Stack remote command done with result:%n"), testDto.getResponse());
        return testDto;
    }
}
