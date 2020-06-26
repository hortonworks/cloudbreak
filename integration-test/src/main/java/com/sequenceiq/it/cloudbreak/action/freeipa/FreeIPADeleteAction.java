package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class FreeIPADeleteAction implements Action<FreeIPATestDto, FreeIPAClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIPADeleteAction.class);

    public FreeIPATestDto action(TestContext testContext, FreeIPATestDto testDto, FreeIPAClient client) throws Exception {
        Log.when(LOGGER, String.format(" FreeIPA crn: %s", testDto.getRequest().getEnvironmentCrn()));
        Log.whenJson(LOGGER, format(" FreeIPA delete:%n"), testDto.getRequest());
        client.getFreeIpaClient()
                .getFreeIpaV1Endpoint()
                .delete(testDto.getRequest().getEnvironmentCrn());
        Log.when(LOGGER, String.format(" FreeIPA deleted successfully. FreeIPA CRN: %s", testDto.getResponse().getCrn()));
        return testDto;
    }
}
