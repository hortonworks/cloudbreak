package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

public class SdxDetailWithResourceAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDetailWithResourceAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.whenJson(LOGGER, "Sdx get request: ", testDto.getRequest());
        SdxClusterDetailResponse response = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getSdxDetailWithResourcesByName(testDto.getName(), Set.of());
        testDto.setResponse(response);
        Log.whenJson(LOGGER, "SdxClusterDetailResponse get response: ",  response);
        return testDto;
    }
}
