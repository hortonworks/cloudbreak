package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCustomTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class SdxSyncCustomAction implements Action<SdxCustomTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxSyncCustomAction.class);

    @Override
    public SdxCustomTestDto action(TestContext testContext, SdxCustomTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, format(" SDX's environment: %s ", testDto.getRequest().getEnvironment()));
        Log.whenJson(LOGGER, " SDX sync custom request: ", testDto.getRequest());
        client.getDefaultClient()
                .sdxEndpoint()
                .sync(testDto.getName());
        Log.when(LOGGER, " SDX sync custom have been initiated. ");
        return testDto;
    }
}
