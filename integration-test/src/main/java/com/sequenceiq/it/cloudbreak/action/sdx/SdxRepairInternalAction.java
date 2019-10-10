package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class SdxRepairInternalAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRepairInternalAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.log(LOGGER, format(" Starting repair on SDX: %s ", client.getSdxClient().sdxEndpoint().get(testDto.getName()).getName()));
        Log.logJSON(LOGGER, " SDX repair request: ", testDto.getSdxRepairRequest());
        client.getSdxClient()
                .sdxEndpoint()
                .repairCluster(testDto.getName(), testDto.getSdxRepairRequest());
        Log.log(LOGGER, " SDX repair have been initiated.");
        return testDto;
    }
}
