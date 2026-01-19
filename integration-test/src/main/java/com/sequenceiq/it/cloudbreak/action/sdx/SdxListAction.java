package com.sequenceiq.it.cloudbreak.action.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxListAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxListAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient(testContext).sdxEndpoint() + ", SDX's environment: "
                + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX list request: ", testDto.getRequest());
        client.getDefaultClient(testContext)
                .sdxEndpoint()
                .list(testDto.getName(), false);
        Log.whenJson(LOGGER, " SDX list response: ", client.getDefaultClient(testContext)
                .sdxEndpoint().list(testContext.get(EnvironmentTestDto.class).getName(), false));
        return testDto;
    }
}
