package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class FreeIPAStopAction implements Action<FreeIPATestDto, FreeIPAClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIPAStopAction.class);

    public FreeIPATestDto action(TestContext testContext, FreeIPATestDto testDto, FreeIPAClient client) throws Exception {
        Log.when(LOGGER, format(" FreeIPA CRN: %s", testDto.getRequest().getEnvironmentCrn()));
        Log.whenJson(LOGGER, format(" FreeIPA stop request: %n"), testDto.getRequest());
        client.getFreeIpaClient()
                .getFreeIpaV1Endpoint()
                .stop(testDto.getRequest().getEnvironmentCrn());
        return testDto;
    }
}
