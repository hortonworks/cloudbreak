package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class FreeIPADescribeAction implements Action<FreeIPATestDto, FreeIPAClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIPADescribeAction.class);

    public FreeIPATestDto action(TestContext testContext, FreeIPATestDto testDto, FreeIPAClient client) throws Exception {
        Log.when(LOGGER, format(" FreeIPA crn: %s", testDto.getRequest().getEnvironmentCrn()));
        Log.whenJson(LOGGER, format(" FreeIPA get request:%n"), testDto.getRequest());
        testDto.setResponse(
                client.getFreeIpaClient()
                        .getFreeIpaV1Endpoint()
                        .describe(testDto.getRequest().getEnvironmentCrn()));
        Log.whenJson(LOGGER, format(" FreeIPA get successfully: %n"), testDto.getResponse());
        Log.when(LOGGER, format(" FreeIPA CRN: %s", testDto.getResponse().getCrn()));
        return testDto;
    }
}
