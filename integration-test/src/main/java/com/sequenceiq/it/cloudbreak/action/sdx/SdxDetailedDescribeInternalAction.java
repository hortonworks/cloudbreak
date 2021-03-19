package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class SdxDetailedDescribeInternalAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDetailedDescribeInternalAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient().sdxEndpoint() + ", SDX's environment: " + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX describe internal request: ", testDto.getRequest());
        testDto.setResponse(client.getDefaultClient()
                .sdxEndpoint()
                .getDetail(testDto.getName(), new HashSet<>()));
        Log.whenJson(LOGGER, " SDX describe internal response: ", client.getDefaultClient().sdxEndpoint().getDetail(testDto.getName(), null));
        return testDto;
    }
}
