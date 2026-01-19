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
import com.sequenceiq.sdx.api.model.SdxRestoreResponse;

public class SdxRestoreInternalAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRestoreInternalAction.class);

    private final String backupId;

    private final String backupLocation;

    public SdxRestoreInternalAction(String backupId, String backupLocation) {
        this.backupId = backupId;
        this.backupLocation = backupLocation;
    }

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        String sdxName = testDto.getName();

        testContext.waitingFor(Duration.ofMinutes(2), "Waiting for CM services to be synchronized has been interrupted");

        Log.when(LOGGER, format(" Internal SDX '%s' restore has been started to '%s' ", sdxName, backupLocation));
        Log.whenJson(LOGGER, " Internal SDX restore request: ", testDto.getRequest());
        LOGGER.info(format(" Internal SDX '%s' restore has been started to '%s'... ", sdxName, backupLocation));
        SdxRestoreResponse sdxRestoreResponse = client.getDefaultClient(testContext)
                .sdxRestoreEndpoint()
                .restoreDatalakeByName(sdxName, backupId, backupLocation, false, false, false, false,
                        0, false);
        testDto.setFlow("SDX restore", sdxRestoreResponse.getFlowIdentifier());
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(sdxName, Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " Internal SDX response after restore: ", client.getDefaultClient(testContext).sdxEndpoint().get(sdxName));
        return testDto;
    }
}
