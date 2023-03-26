package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCustomTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

public class SdxDeleteCustomAction implements Action<SdxCustomTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDeleteCustomAction.class);

    @Override
    public SdxCustomTestDto action(TestContext testContext, SdxCustomTestDto testDto, SdxClient client) throws Exception {
        testContext.skipSafeLogicValidation();
        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient().sdxEndpoint() + ", SDX's environment: " + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX custom delete request: ", testDto.getRequest());
        FlowIdentifier flowIdentifier = client.getDefaultClient()
                .sdxEndpoint()
                .delete(testDto.getName(), false);
        testDto.setFlow("SDX delete", flowIdentifier);
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient()
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX custom delete response: ", detailedResponse);
        return testDto;
    }
}
