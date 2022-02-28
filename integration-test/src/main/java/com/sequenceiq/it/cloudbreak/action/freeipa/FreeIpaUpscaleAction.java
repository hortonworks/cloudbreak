package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpscaleRequest;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUpscaleTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class FreeIpaUpscaleAction implements Action<FreeIpaUpscaleTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUpscaleAction.class);

    @Override
    public FreeIpaUpscaleTestDto action(TestContext testContext, FreeIpaUpscaleTestDto testDto, FreeIpaClient client) throws Exception {
        Log.whenJson(LOGGER, format(" FreeIPA upscale request:%n"), testDto.getRequest());
        UpscaleRequest upscaleRequest = new UpscaleRequest();
        upscaleRequest.setEnvironmentCrn(testDto.getRequest().getEnvironmentCrn());
        upscaleRequest.setTargetAvailabilityType(testDto.getRequest().getTargetAvailabilityType());
        testDto.setResponse(client.getDefaultClient()
                .getFreeIpaV1Endpoint()
                .upscale(upscaleRequest));
        testDto.setFlow("FreeIPA upscale",  testDto.getResponse().getFlowIdentifier());
        testDto.setOperationId(testDto.getResponse().getOperationId());
        Log.whenJson(LOGGER, format(" FreeIPA upscale started: %n"), testDto.getResponse());
        return testDto;
    }
}
