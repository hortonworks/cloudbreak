package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

public class SdxCreateInternalAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCreateInternalAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, " SDX endpoint: %s" + client.getSdxClient().sdxEndpoint() + ", SDX's environment: " + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX create request: ", testDto.getRequest());
        SdxClusterResponse sdxClusterResponse = client.getSdxClient()
                .sdxInternalEndpoint()
                .create(testDto.getName(), testDto.getRequest());
        testDto.setFlow("SDX create internal", sdxClusterResponse.getFlowIdentifier());
        SdxClusterDetailResponse detailedResponse = client.getSdxClient()
                .sdxEndpoint()
                .getDetailByCrn(sdxClusterResponse.getCrn(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX create response: ", client.getSdxClient().sdxEndpoint().get(testDto.getName()));
        return testDto;
    }
}
