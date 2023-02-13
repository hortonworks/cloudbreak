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
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

public class SdxUpgradeAction implements Action<SdxTestDto, SdxClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        SdxUpgradeRequest upgradeRequest = testDto.getSdxUpgradeRequest();

        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient().sdxEndpoint() + ", SDX's environment: " + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX upgrade request: ", upgradeRequest);
        SdxUpgradeResponse upgradeResponse = client.getDefaultClient()
                .sdxUpgradeEndpoint()
                .upgradeClusterByName(testDto.getName(), upgradeRequest);
        testDto.setFlow("SDX upgrade", upgradeResponse.getFlowIdentifier());
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient()
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX upgrade response: ", detailedResponse);
        return testDto;
    }
}
