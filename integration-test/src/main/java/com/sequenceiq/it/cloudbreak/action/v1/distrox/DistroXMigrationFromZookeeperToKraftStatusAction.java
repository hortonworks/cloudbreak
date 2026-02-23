package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import static java.lang.String.format;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.KraftMigrationStatusResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXMigrationFromZookeeperToKraftStatusAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXMigrationFromZookeeperToKraftStatusAction.class);

    private final String desiredStatus;

    public DistroXMigrationFromZookeeperToKraftStatusAction(String desiredStatus) {
        this.desiredStatus = desiredStatus;
    }

    public DistroXMigrationFromZookeeperToKraftStatusAction() {
        this.desiredStatus = null;
    }

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, format(" Distrox get KRaft migration status: %s ", testDto.getName()));
        Log.whenJson(LOGGER, " Distrox get KRaft migration status request: ", testDto.getRequest());
        KraftMigrationStatusResponse response = client.getDefaultClient(testContext)
                .distroXKraftMigrationV1Endpoint()
                .zookeeperToKraftMigrationStatusByCrn(testDto.getCrn());
        StackV4Response stackV4Response = client.getDefaultClient(testContext)
                .distroXV1Endpoint().getByName(testDto.getName(), Collections.emptySet());
        testDto.setResponse(stackV4Response);
        if (desiredStatus != null && !response.getKraftMigrationStatus().equals(desiredStatus)) {
            throw new TestFailException("Kraft Migration Status are mismatched: expected: " + desiredStatus + ", got: " + response.getKraftMigrationStatus());
        }
        Log.whenJson(LOGGER, " Distrox get KRaft migration status response: ", response);
        return testDto;
    }
}
