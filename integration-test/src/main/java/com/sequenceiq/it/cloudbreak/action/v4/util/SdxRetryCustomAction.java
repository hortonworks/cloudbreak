package com.sequenceiq.it.cloudbreak.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCustomTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxRetryCustomAction implements Action<SdxCustomTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRetryCustomAction.class);

    @Override
    public SdxCustomTestDto action(TestContext testContext, SdxCustomTestDto testDto, SdxClient sdxClient)
            throws Exception {
        sdxClient.getDefaultClient().sdxEndpoint().retryByCrn(testDto.getCrn());
        return testDto;
    }
}
