package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.CreateFreeIpaV1Response;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaCreateAction extends AbstractFreeIpaAction<FreeIpaTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreateAction.class);

    public FreeIpaTestDto freeIpaAction(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        Log.whenJson(LOGGER, format(" FreeIPA post request:%n"), testDto.getRequest());
        if (!StringUtils.equals(testContext.getExistingResourceNames().get(FreeIpaTestDto.class), testDto.getName())) {
            CreateFreeIpaV1Response createFreeIpaV1Response = client.getDefaultClient(testContext).getFreeIpaV1Endpoint().create(testDto.getRequest());
            testDto.setResponse(createFreeIpaV1Response);
            testDto.setFlow("freeIpaCreateFlow", createFreeIpaV1Response.getFlowIdentifier());
        } else {
            Log.when(LOGGER, format(" FreeIPA already exists: %s", testDto.getName()));
            testDto.setResponse(client.getDefaultClient(testContext).getFreeIpaV1Endpoint().describe(testContext.given(EnvironmentTestDto.class).getCrn()));
        }
        Log.whenJson(LOGGER, format(" FreeIPA created  successfully:%n"), testDto.getResponse());
        Log.when(LOGGER, format(" FreeIPA CRN: %s", testDto.getResponse().getCrn()));
        return testDto;
    }
}
