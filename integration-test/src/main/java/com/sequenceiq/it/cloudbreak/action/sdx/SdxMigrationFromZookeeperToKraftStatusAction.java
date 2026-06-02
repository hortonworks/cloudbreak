package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.distrox.api.v1.distrox.model.KraftMigrationStatusResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

public class SdxMigrationFromZookeeperToKraftStatusAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxMigrationFromZookeeperToKraftStatusAction.class);

    private final String desiredStatus;

    public SdxMigrationFromZookeeperToKraftStatusAction(String desiredStatus) {
        this.desiredStatus = desiredStatus;
    }

    public SdxMigrationFromZookeeperToKraftStatusAction() {
        this.desiredStatus = null;
    }

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, format(" Sdx get KRaft migration status: %s ", testDto.getName()));
        Log.whenJson(LOGGER, " Sdx get KRaft migration status request: ", testDto.getRequest());
        KraftMigrationStatusResponse response = client.getDefaultClient(testContext)
                .sdxKraftMigrationEndpoint()
                .zookeeperToKraftMigrationStatusByCrn(testDto.getCrn());
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        if (desiredStatus != null && !response.getKraftMigrationStatus().equals(desiredStatus)) {
            throw new TestFailException("Kraft Migration Status are mismatched: expected: " + desiredStatus + ", got: " + response.getKraftMigrationStatus());
        }
        Log.whenJson(LOGGER, " Sdx get KRaft migration status response: ", detailedResponse);
        return testDto;
    }
}
