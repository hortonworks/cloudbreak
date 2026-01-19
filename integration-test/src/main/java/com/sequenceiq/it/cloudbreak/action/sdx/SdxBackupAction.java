package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import java.time.Duration;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxBackupResponse;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

public class SdxBackupAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxBackupAction.class);

    private final String backupLocation;

    private final String backupName;

    public SdxBackupAction(String backupLocation, String backupName) {
        this.backupLocation = backupLocation;
        this.backupName = backupName;
    }

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        String sdxName = testDto.getName();

        testContext.waitingFor(Duration.ofMinutes(2), "Waiting for CM services to be synchronized has been interrupted");

        Log.when(LOGGER, format(" SDX '%s' backup has been started to '%s' by name '%s'... ", sdxName, backupLocation, backupName));
        Log.whenJson(LOGGER, " SDX backup request: ", testDto.getRequest());
        LOGGER.info(format(" SDX '%s' backup has been started to '%s' by name '%s'... ", sdxName, backupLocation, backupName));
        SdxBackupResponse sdxBackupResponse = client.getDefaultClient(testContext)
                .sdxBackupEndpoint()
                .backupDatalakeByName(sdxName, backupLocation, backupName, false, false, false, false,
                        0);
        testDto.setFlow("SDX backup", sdxBackupResponse.getFlowIdentifier());
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(sdxName, Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX response after backup: ", client.getDefaultClient(testContext).sdxEndpoint().get(sdxName));
        return testDto;
    }
}
