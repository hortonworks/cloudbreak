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
import com.sequenceiq.sdx.api.model.SdxBackupResponse;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

public class SdxBackupInternalAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxBackupInternalAction.class);

    private final String backupLocation;

    private final String backupName;

    public SdxBackupInternalAction(String backupLocation, String backupName) {
        this.backupLocation = backupLocation;
        this.backupName = backupName;
    }

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        String sdxName = testDto.getName();

        sleep(2, sdxName);

        Log.when(LOGGER, format(" Internal SDX '%s' backup has been started to '%s' by name '%s'... ", sdxName, backupLocation, backupName));
        Log.whenJson(LOGGER, " Internal SDX backup request: ", testDto.getRequest());
        LOGGER.info(format(" Internal SDX '%s' backup has been started to '%s' by name '%s'... ", sdxName, backupLocation, backupName));
        SdxBackupResponse sdxBackupResponse = client.getDefaultClient()
                .sdxBackupEndpoint()
                .backupDatalakeByName(sdxName, backupLocation, backupName);
        testDto.setFlow("SDX backup", sdxBackupResponse.getFlowIdentifier());
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient()
                .sdxEndpoint()
                .getDetail(sdxName, Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " Internal SDX response after backup: ", client.getDefaultClient().sdxEndpoint().get(sdxName));
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
