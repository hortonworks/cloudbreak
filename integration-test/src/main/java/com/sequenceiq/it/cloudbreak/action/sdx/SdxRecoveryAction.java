package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

public class SdxRecoveryAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRecoveryAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        SdxRecoveryRequest recoveryRequest = testDto.getSdxRecoveryRequest();

        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient().sdxEndpoint() + ", SDX's environment: " + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX recovery request: ", recoveryRequest);
        SdxRecoveryResponse recoveryResponse = client.getDefaultClient()
                .sdxRecoveryEndpoint()
                .recoverClusterByName(testDto.getName(), recoveryRequest);
        testDto.setFlow("SDX recovery", recoveryResponse.getFlowIdentifier());
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient()
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX recovery response: ", detailedResponse);
        return testDto;
    }
}
