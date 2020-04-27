package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.FlowUtil;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;

public class SdxOsUpgradeAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxOsUpgradeAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.log(LOGGER, format(" Environment: %s", testDto.getRequest().getEnvironment()));
        Log.whenJson(LOGGER, " SDX upgrade request: ", testDto.getRequest());
        SdxUpgradeRequest upgradeRequest = new SdxUpgradeRequest();
        upgradeRequest.setLockComponents(true);
        FlowIdentifier flowIdentifier = client.getSdxClient()
                .sdxUpgradeEndpoint()
                .upgradeClusterByName(testDto.getName(), upgradeRequest).getFlowIdentifier();
        FlowUtil.setFlow("SDX upgrade", testDto, flowIdentifier, client);
        Log.log(LOGGER, " SDX name: %s", client.getSdxClient().sdxEndpoint().get(testDto.getName()).getName());
        return testDto;
    }
}
