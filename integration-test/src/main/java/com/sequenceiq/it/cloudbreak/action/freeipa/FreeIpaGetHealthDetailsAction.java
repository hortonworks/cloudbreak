package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaHealthDetailsDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaGetHealthDetailsAction implements Action<FreeIpaHealthDetailsDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaGetHealthDetailsAction.class);

    @Override
    public FreeIpaHealthDetailsDto action(TestContext testContext, FreeIpaHealthDetailsDto testDto, FreeIpaClient client) throws Exception {
        Log.when(LOGGER, format(" Getting FreeIpa Health Details for environment: [%s] and freeIpa: %s", testDto.getEnvironmentCrn(),
                testDto.getFreeIpaCrn()));
        testDto.setResponse(client.getDefaultClient()
                .getFreeIpaV1Endpoint()
                .healthDetails(testDto.getEnvironmentCrn()));
        Log.whenJson(LOGGER, format(" FreeIpa Health Details respone: %n"), testDto.getResponse());
        return testDto;
    }
}