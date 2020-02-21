package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.FlowUtil;

public class SdxRepairAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRepairAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, format(" Starting repair on SDX: %s ", testDto.getName()));
        Log.whenJson(LOGGER, " SDX repair request: ", testDto.getSdxRepairRequest());
        FlowIdentifier flowIdentifier = client.getSdxClient()
                .sdxEndpoint()
                .repairCluster(testDto.getName(), testDto.getSdxRepairRequest());
        FlowUtil.setFlow("SDX repair", testDto, flowIdentifier, client);
        Log.when(LOGGER, " SDX repair have been initiated.");
        return testDto;
    }
}
