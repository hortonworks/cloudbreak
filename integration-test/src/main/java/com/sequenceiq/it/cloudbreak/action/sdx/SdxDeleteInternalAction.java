package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class SdxDeleteInternalAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDeleteInternalAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.log(LOGGER, format(" Environment: %s", testDto.getRequest().getEnvironment()));
        Log.logJSON(LOGGER, " SDX delete request: ", testDto.getRequest());
        client.getSdxClient()
                .sdxEndpoint()
                .delete(testDto.getName());
        Log.logJSON(LOGGER, " SDX list response: ", client.getSdxClient().sdxEndpoint().list(testContext.get(EnvironmentTestDto.class).getName()));
        return testDto;
    }
}
