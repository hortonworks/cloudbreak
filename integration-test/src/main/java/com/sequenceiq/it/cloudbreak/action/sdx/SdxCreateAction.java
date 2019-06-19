package com.sequenceiq.it.cloudbreak.action.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class SdxCreateAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCreateAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        Log.log(LOGGER, " Environment: %s", testDto.getRequest().getEnvironment());
        Log.log(LOGGER, " SDX endpoint: %s", client.getSdxClient().sdxEndpoint().toString());
        Log.logJSON(LOGGER, " SDX create request: ", testDto.getRequest());
        client.getSdxClient()
                .sdxEndpoint()
                .create(testDto.getName(), testDto.getRequest());
        Log.logJSON(LOGGER, " SDX describe response: ", client.getSdxClient().sdxEndpoint().get(testDto.getName()));
        Log.log(LOGGER, " SDX name: %s", client.getSdxClient().sdxEndpoint().get(testDto.getName()).getName());
        return testDto;
    }
}
