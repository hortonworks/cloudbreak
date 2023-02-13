package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

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

public class SdxStopAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxStopAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, format(" Stop SDX: %s ", testDto.getName()));
        Log.whenJson(LOGGER, " SDX stop request: ", testDto.getRequest());
        FlowIdentifier flowIdentifier = client.getDefaultClient().sdxEndpoint().stopByName(testDto.getName());
        testDto.setFlow("SDX stop", flowIdentifier);
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient()
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX stop response: ", detailedResponse);
        return testDto;
    }
}
