package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.sdx.api.model.SdxBackupResponse;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

public class SdxBackupAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxBackupAction.class);

    private final String backupLocation;

    private final String backupName;

    public SdxBackupAction(String backupLocation, String backupName) {
        this.backupLocation = backupLocation;
        this.backupName = backupName;
    }

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        String sdxName = testDto.getName();

        Log.when(LOGGER, format(" SDX '%s' backup has been started to '%s' as '%s' ", sdxName, backupLocation, backupName));
        Log.whenJson(LOGGER, " SDX backup request: ", testDto.getRequest());
        LOGGER.info(format(" SDX '%s' backup has been started to '%s' as '%s'... ", sdxName, backupLocation, backupName));
        SdxBackupResponse sdxBackupResponse = client.getDefaultClient()
                .sdxEndpoint()
                .backupDatalakeByName(sdxName, backupLocation, backupName);
        testDto.setFlow("SDX backup", sdxBackupResponse.getFlowIdentifier());

        SdxClusterDetailResponse detailedResponse = client.getDefaultClient()
                .sdxEndpoint()
                .getDetail(sdxName, Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX backup response: ", client.getDefaultClient().sdxEndpoint().get(sdxName));
        return testDto;
    }
}
