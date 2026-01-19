package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxDescribeAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDescribeAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, format(" SDX environment Crn: %s ", testDto.getRequest().getEnvironment()));
        Log.whenJson(LOGGER, " SDX get details request: ", testDto.getRequest());
        testDto.setResponse(client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet()));
        Log.whenJson(LOGGER, " SDX get details response: ", testDto.getResponse());
        Log.when(LOGGER, format(" SDX Crn: %s", testDto.getResponse().getCrn()));
        return testDto;
    }
}
