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
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerResponse;

public class SdxUpgradeDatabaseServerAction implements Action<SdxTestDto, SdxClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeDatabaseServerAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        SdxUpgradeDatabaseServerRequest upgradeDatabaseServerRequest = testDto.getSdxUpgradeDatabaseServerRequest();

        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient(testContext).sdxEndpoint() + ", SDX's environment: "
                + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX upgrade database server request: ", upgradeDatabaseServerRequest);
        SdxUpgradeDatabaseServerResponse upgradeDatabaseServerResponse = client.getDefaultClient(testContext)
                .sdxUpgradeEndpoint()
                .upgradeDatabaseServerByName(testDto.getName(), upgradeDatabaseServerRequest);
        testDto.setFlow("SDX upgrade database server", upgradeDatabaseServerResponse.getFlowIdentifier());
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX upgrade database server response: ", detailedResponse);
        return testDto;
    }
}
