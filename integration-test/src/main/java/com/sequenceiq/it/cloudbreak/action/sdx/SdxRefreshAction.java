package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class SdxRefreshAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRefreshAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        Log.log(LOGGER, format(" Refresh SDX: %s ", client.getSdxClient().sdxEndpoint().get(testDto.getName()).getName()));
        testDto.setResponse(
                client.getSdxClient().sdxEndpoint().getDetail(testDto.getName(), Collections.emptySet())
        );
        Log.log(LOGGER, " SDX refresh have been initiated. ");
        return testDto;
    }
}
