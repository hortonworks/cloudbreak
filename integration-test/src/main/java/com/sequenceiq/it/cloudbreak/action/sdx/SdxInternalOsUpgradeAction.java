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
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

public class SdxInternalOsUpgradeAction implements Action<SdxInternalTestDto, SdxClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxInternalOsUpgradeAction.class);

    private static String imageId;

    public SdxInternalOsUpgradeAction(String imageId) {
        this.imageId = imageId;
    }

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        SdxUpgradeRequest upgradeRequest = testDto.getSdxUpgradeRequest();
        upgradeRequest.setRuntime(null);
        upgradeRequest.setSkipBackup(true);
        upgradeRequest.setImageId(imageId);

        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient(testContext).sdxEndpoint() + ", SDX's environment: "
                + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX OS upgrade request: ", upgradeRequest);
        SdxUpgradeResponse upgradeResponse = client.getDefaultClient(testContext)
                .sdxUpgradeEndpoint()
                .upgradeClusterByName(testDto.getName(), upgradeRequest);
        testDto.setFlow("SDX OS upgrade", upgradeResponse.getFlowIdentifier());
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX upgrade response: ", detailedResponse);
        return testDto;
    }
}