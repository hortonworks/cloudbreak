package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

public class SdxMigrationFromZookeeperToKraftRollbackAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxMigrationFromZookeeperToKraftRollbackAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, format(" Sdx rollback KRaft migration: %s ", testDto.getName()));
        Log.whenJson(LOGGER, " Sdx rollback KRaft migration request: ", testDto.getRequest());
        FlowIdentifier flow = client.getDefaultClient(testContext)
                .sdxKraftMigrationEndpoint()
                .rollbackMigrationFromZookeeperToKraftByCrn(testDto.getCrn());
        testDto.setFlow("Sdx rollback KRaft migration", flow);

        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " Sdx rollback KRaft migration response: ", detailedResponse);
        return testDto;
    }
}
