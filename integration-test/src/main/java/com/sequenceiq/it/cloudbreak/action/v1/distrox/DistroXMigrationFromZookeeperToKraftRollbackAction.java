package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import static java.lang.String.format;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXMigrationFromZookeeperToKraftRollbackAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXMigrationFromZookeeperToKraftRollbackAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, format(" Distrox rollback KRaft migration: %s ", testDto.getName()));
        Log.whenJson(LOGGER, " Distrox rollback KRaft migration request: ", testDto.getRequest());
        FlowIdentifier flow = client.getDefaultClient(testContext)
                .distroXKraftMigrationV1Endpoint()
                .rollbackMigrationFromZookeeperToKraftByCrn(testDto.getCrn());
        testDto.setFlow("Distrox rollback KRaft migration", flow);
        StackV4Response stackV4Response = client.getDefaultClient(testContext)
                .distroXV1Endpoint().getByName(testDto.getName(), Collections.emptySet());
        testDto.setResponse(stackV4Response);
        Log.whenJson(LOGGER, " Distrox rollback KRaft migration response: ", stackV4Response);
        return testDto;
    }
}
