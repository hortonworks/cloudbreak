package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxScaleTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

public class SdxScaleAction implements Action<SdxScaleTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxScaleAction.class);

    @Override
    public SdxScaleTestDto action(TestContext testContext, SdxScaleTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, format(" Scaling SDX: %s ", testDto.getName()));
        Log.whenJson(LOGGER, " Scaling SDX request: ", testDto.getRequest());
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext).sdxEndpoint().horizontalScaleByName(testDto.getName(),
                testDto.getRequest());
        testDto.setFlow("SDX scale", flowIdentifier);
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX scale response: ", detailedResponse);
        return testDto;
    }
}
