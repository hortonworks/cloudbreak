package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class FreeIPACreateAction implements Action<FreeIPATestDto, FreeIPAClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIPACreateAction.class);

    public FreeIPATestDto action(TestContext testContext, FreeIPATestDto testDto, FreeIPAClient client) throws Exception {
        Log.whenJson(LOGGER, format(" FreeIPA post request:%n"), testDto.getRequest());
        testDto.setResponse(
                client.getFreeIpaClient()
                        .getFreeIpaV1Endpoint()
                        .create(testDto.getRequest()));
        Log.whenJson(LOGGER, format(" FreeIPA created  successfully:%n"), testDto.getResponse());
        Log.when(LOGGER, format(" FreeIPA CRN: %s", testDto.getResponse().getCrn()));
        return testDto;
    }
}
