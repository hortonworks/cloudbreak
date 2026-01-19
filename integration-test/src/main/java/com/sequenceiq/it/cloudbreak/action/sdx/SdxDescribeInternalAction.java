package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxDescribeInternalAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDescribeInternalAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        if (testDto.getResponse() != null) {
            Log.when(LOGGER, format(" SDX Crn: %s ", testDto.getCrn()));
        }
        Log.whenJson(LOGGER, " SDX describe internal request: ", testDto.getRequest());
        testDto.setResponse(client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet()));
        Log.whenJson(LOGGER, " SDX describe internal response: ", testDto.getResponse());
        return testDto;
    }
}
