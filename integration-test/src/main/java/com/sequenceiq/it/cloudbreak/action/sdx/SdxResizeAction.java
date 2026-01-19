package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

public class SdxResizeAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxResizeAction.class);

    /**
     * Starts SDX resize operation, and retrieves the detailed SDX response after it's finished.
     * @return result of the resize
     * @throws Exception if the SdxClusterDetailResponse cannot be converted to JSON
     */
    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        SdxClusterResizeRequest clusterResizeRequest = testDto.getSdxResizeRequest();

        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient(testContext).sdxEndpoint() + ", SDX's environment: "
                + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX resize request: ", clusterResizeRequest);
        SdxClusterResponse sdxClusterResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .resize(testDto.getName(), clusterResizeRequest);
        testDto.setFlow("SDX resize", sdxClusterResponse.getFlowIdentifier());
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX resize response: ", detailedResponse);
        return testDto;
    }
}
