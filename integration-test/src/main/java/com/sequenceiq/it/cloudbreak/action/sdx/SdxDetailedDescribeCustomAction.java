package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCustomTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class SdxDetailedDescribeCustomAction implements Action<SdxCustomTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDetailedDescribeCustomAction.class);

    @Override
    public SdxCustomTestDto action(TestContext testContext, SdxCustomTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient().sdxEndpoint() + ", SDX's environment: " + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX describe detailed custom request: ", testDto.getRequest());
        testDto.setResponse(client.getDefaultClient()
                .sdxEndpoint()
                .getDetail(testDto.getName(), new HashSet<>()));
        Log.whenJson(LOGGER, " SDX describe detailed custom response: ", client.getDefaultClient().sdxEndpoint().getDetail(testDto.getName(), null));
        return testDto;
    }
}
