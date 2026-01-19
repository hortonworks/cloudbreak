package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import java.time.Duration;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

public class SdxSyncInternalAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxSyncInternalAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        String sdxName = testDto.getName();

        testContext.waitingFor(Duration.ofMinutes(1), "Waiting for CM services to be synchronized has been interrupted");

        Log.when(LOGGER, format(" Internal SDX '%s' sync has been started... ", sdxName));
        Log.whenJson(LOGGER, " Internal SDX sync request: ", testDto.getRequest());
        LOGGER.info(format(" Internal SDX '%s' sync has been started... ", sdxName));
        client.getDefaultClient(testContext)
                .sdxEndpoint()
                .sync(sdxName);
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(sdxName, Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " Internal SDX response after sync: ", client.getDefaultClient(testContext).sdxEndpoint().get(sdxName));

        return testDto;
    }
}
