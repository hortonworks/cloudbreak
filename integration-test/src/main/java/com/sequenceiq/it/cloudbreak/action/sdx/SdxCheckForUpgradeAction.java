package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

public class SdxCheckForUpgradeAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCheckForUpgradeAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        Log.log(LOGGER, format(" Environment: %s", testDto.getRequest().getEnvironment()));
        Log.whenJson(LOGGER, " SDX check for upgrade request: ", testDto.getRequest());
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setDryRun(true);
        SdxUpgradeResponse upgradeResponse = client.getDefaultClient(testContext)
                .sdxUpgradeEndpoint()
                .upgradeClusterByName(testDto.getName(), request);
        Log.whenJson(LOGGER, " SDX check for upgrade response: ", upgradeResponse);
        Log.log(LOGGER, " SDX name: %s", client.getDefaultClient(testContext).sdxEndpoint().get(testDto.getName()).getName());
        return testDto;
    }
}
