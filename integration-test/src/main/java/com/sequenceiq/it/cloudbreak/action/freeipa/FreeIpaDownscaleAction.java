package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleRequest;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaDownscaleTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class FreeIpaDownscaleAction implements Action<FreeIpaDownscaleTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDownscaleAction.class);

    @Override
    public FreeIpaDownscaleTestDto action(TestContext testContext, FreeIpaDownscaleTestDto testDto, FreeIpaClient client) throws Exception {
        Log.whenJson(LOGGER, format(" FreeIPA downscale request:%n"), testDto.getRequest());
        DownscaleRequest downscaleRequest = new DownscaleRequest();
        downscaleRequest.setEnvironmentCrn(testDto.getRequest().getEnvironmentCrn());
        downscaleRequest.setTargetFormFactor(testDto.getRequest().getTargetFormFactor());
        testDto.setResponse(client.getDefaultClient()
                .getFreeIpaV1Endpoint()
                .downscale(downscaleRequest));
        testDto.setFlow("FreeIPA downscale",  testDto.getResponse().getFlowIdentifier());
        testDto.setOperationId(testDto.getResponse().getOperationId());
        Log.whenJson(LOGGER, format(" FreeIPA downscale started: %n"), testDto.getResponse());
        return testDto;
    }
}
