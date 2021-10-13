package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
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

        sleep(2, sdxName);

        Log.when(LOGGER, format(" Internal SDX '%s' restore has been started to '%s' ", sdxName, backupLocation));
        Log.whenJson(LOGGER, " Internal SDX restore request: ", testDto.getRequest());
        LOGGER.info(format(" Internal SDX '%s' restore has been started to '%s'... ", sdxName, backupLocation));
        SdxRestoreResponse sdxRestoreResponse = client.getDefaultClient()
                .sdxRestoreEndpoint()
                .restoreDatalakeByName(sdxName, backupId, backupLocation);
        testDto.setFlow("SDX restore", sdxRestoreResponse.getFlowIdentifier());
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient()
                .sdxEndpoint()
                .getDetail(sdxName, Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " Internal SDX response after restore: ", client.getDefaultClient().sdxEndpoint().get(sdxName));
        return testDto;
    }

    private void sleep(long sleepMinutes, String sdxName) {
        try {
            TimeUnit.MINUTES.sleep(sleepMinutes);
        } catch (InterruptedException ignored) {
            LOGGER.warn("Waiting for CM services to be synchronized has been interrupted, cause:", ignored);
        }
    }
}
