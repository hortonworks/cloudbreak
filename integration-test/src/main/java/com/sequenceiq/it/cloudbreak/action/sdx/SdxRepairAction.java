package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxRepairTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class SdxRepairAction implements Action<SdxRepairTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRepairAction.class);

    @Override
    public SdxRepairTestDto action(TestContext testContext, SdxRepairTestDto testDto, SdxClient client) throws Exception {
        Log.log(LOGGER, format(" Starting repair on SDX: %s ", client.getSdxClient().sdxEndpoint().get(testDto.getName()).getName()));
        Log.logJSON(LOGGER, " SDX repair request: ", testDto.getRequest());
        client.getSdxClient()
                .sdxEndpoint()
                .repairCluster(testDto.getName(), testDto.getRequest());
        Log.log(LOGGER, " SDX repair have been initiated.");
        return testDto;
    }
}
