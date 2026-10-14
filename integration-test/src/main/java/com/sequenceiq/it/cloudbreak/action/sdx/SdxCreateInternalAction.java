package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

public class SdxCreateInternalAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCreateInternalAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, " Data Lake endpoint: %s" + client.getDefaultClient(testContext).sdxEndpoint() + ", Data Lake's environment: "
                + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " Data Lake create request: ", testDto.getRequest());
        if (!StringUtils.equals(testContext.getExistingResourceNames().get(SdxInternalTestDto.class), testDto.getName())) {
            SdxClusterResponse sdxClusterResponse = client.getDefaultClient(testContext).sdxInternalEndpoint().create(testDto.getName(), testDto.getRequest());
            testDto.setFlow("Data Lake create internal", sdxClusterResponse.getFlowIdentifier());
            testDto.setResponse(client.getDefaultClient(testContext).sdxEndpoint().getDetailByCrn(sdxClusterResponse.getCrn(), Collections.emptySet()));
        } else {
            Log.whenJson(LOGGER, " Data Lake already exists %s.", testDto.getName());
            testDto.setResponse(client.getDefaultClient(testContext).sdxEndpoint().getDetail(testDto.getName(), Collections.emptySet()));
        }
        Log.whenJson(LOGGER, " Data Lake create response: ", testDto.getResponse());
        return testDto;
    }
}
