package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaChildEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaAttachChildEnvironmentAction implements Action<FreeIpaChildEnvironmentTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaAttachChildEnvironmentAction.class);

    @Override
    public FreeIpaChildEnvironmentTestDto action(TestContext testContext, FreeIpaChildEnvironmentTestDto testDto, FreeIpaClient client) throws Exception {
        Log.whenJson(LOGGER, format(" FreeIPA attach child environment:%n"), testDto.getRequest());
        client.getDefaultClient(testContext)
                .getFreeIpaV1Endpoint()
                .attachChildEnvironment(testDto.getRequest());
        Log.when(LOGGER, " FreeIPA attached child environment successfully.");
        return testDto;
    }
}
