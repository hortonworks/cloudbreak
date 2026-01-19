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

public class SdxOsUpgradeAction implements Action<SdxTestDto, SdxClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxOsUpgradeAction.class);

    private static String imageId;

    public SdxOsUpgradeAction(String imageId) {
        this.imageId = imageId;
    }

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        SdxUpgradeRequest upgradeRequest = testDto.getSdxUpgradeRequest();
        upgradeRequest.setRuntime(null);
        upgradeRequest.setSkipBackup(true);
        upgradeRequest.setImageId(imageId);

        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient(testContext).sdxEndpoint() + ", SDX's environment: "
                + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX upgrade request: ", upgradeRequest);
        SdxUpgradeResponse upgradeResponse = client.getDefaultClient(testContext)
                .sdxUpgradeEndpoint()
                .upgradeClusterByName(testDto.getName(), upgradeRequest);
        testDto.setFlow("SDX upgrade", upgradeResponse.getFlowIdentifier());
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX upgrade response: ", detailedResponse);
        return testDto;
    }
}