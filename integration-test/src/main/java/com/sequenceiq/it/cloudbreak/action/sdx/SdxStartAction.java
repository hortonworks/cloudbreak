package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class SdxStartAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxStartAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, format(" Start SDX: %s ", testDto.getName()));
        client.getSdxClient().sdxEndpoint().startByName(testDto.getName());
        Log.when(LOGGER, " SDX Start have been initiated. ");
        return testDto;
    }
}
