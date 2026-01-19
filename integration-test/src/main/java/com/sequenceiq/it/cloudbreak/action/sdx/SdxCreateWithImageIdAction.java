package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

public class SdxCreateWithImageIdAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCreateWithImageIdAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient(testContext).sdxEndpoint() + ", SDX's environment: "
                + testDto.getRequest().getEnvironment());
        testDto.getRequest().setRuntime(null);
        Log.whenJson(LOGGER, " SDX create request: ", testDto.getRequest());
        SdxClusterResponse sdxClusterResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .create(testDto.getName(), testDto.getRequest());
        testDto.setFlow("SDX create", sdxClusterResponse.getFlowIdentifier());
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetailByCrn(sdxClusterResponse.getCrn(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX create response: ", client.getDefaultClient(testContext).sdxEndpoint().get(testDto.getName()));
        return testDto;
    }
}