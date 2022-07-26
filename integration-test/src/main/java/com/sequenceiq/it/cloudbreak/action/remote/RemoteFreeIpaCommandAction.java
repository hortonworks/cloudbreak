package com.sequenceiq.it.cloudbreak.action.remote;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaRemoteTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class RemoteFreeIpaCommandAction implements Action<FreeIpaRemoteTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteFreeIpaCommandAction.class);

    public FreeIpaRemoteTestDto action(TestContext testContext, FreeIpaRemoteTestDto testDto, FreeIpaClient client) throws Exception {
        String freeIpaCrn = client.getDefaultClient().getFreeIpaV1Endpoint().describe(testDto.getEnvironmentCrn()).getCrn();
        Log.when(LOGGER, format(" Remote command [%s] is going to be executed at freeIpa: '%s' in environment: '%s'... ", testDto.getRequest().getCommand(),
                freeIpaCrn, testDto.getEnvironmentCrn()));
        Log.whenJson(LOGGER, format(" FreeIPA remote command POST request:%n"), testDto.getRequest());
        testDto.setResponse(
                client.getDefaultClient()
                        .utilV1Endpoint()
                        .remoteCommandExecution(testDto.getEnvironmentCrn(), testDto.getRequest()));
        Log.whenJson(LOGGER, format(" FreeIPA remote command done with result:%n"), testDto.getResponse());
        return testDto;
    }
}
