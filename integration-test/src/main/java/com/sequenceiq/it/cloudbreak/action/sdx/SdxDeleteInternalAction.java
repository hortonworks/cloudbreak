package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

public class SdxDeleteInternalAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDeleteInternalAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        testContext.skipSafeLogicValidation();
        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient(testContext).sdxEndpoint() + ", SDX's environment: "
                + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX Internal delete request: ", testDto.getRequest());
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .delete(testDto.getName(), false);
        testDto.setFlow("SDX delete", flowIdentifier);
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX Internal delete response: ", detailedResponse);
        return testDto;
    }
}
