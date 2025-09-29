package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleRequest;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaDownscaleTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaDownscaleAction extends AbstractFreeIpaAction<FreeIpaDownscaleTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDownscaleAction.class);

    @Override
    public FreeIpaDownscaleTestDto freeIpaAction(TestContext testContext, FreeIpaDownscaleTestDto testDto, FreeIpaClient client) throws Exception {
        Log.whenJson(LOGGER, format(" FreeIPA downscale request:%n"), testDto.getRequest());
        DownscaleRequest downscaleRequest = new DownscaleRequest();
        downscaleRequest.setEnvironmentCrn(testDto.getRequest().getEnvironmentCrn());
        downscaleRequest.setTargetAvailabilityType(testDto.getRequest().getTargetAvailabilityType());
        testDto.setResponse(client.getDefaultClient()
                .getFreeIpaV1Endpoint()
                .downscale(downscaleRequest));
        testDto.setFlow("FreeIPA downscale",  testDto.getResponse().getFlowIdentifier());
        testDto.setOperationId(testDto.getResponse().getOperationId());
        Log.whenJson(LOGGER, format(" FreeIPA downscale started: %n"), testDto.getResponse());
        return testDto;
    }
}
