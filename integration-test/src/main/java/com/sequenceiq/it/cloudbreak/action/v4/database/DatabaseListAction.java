package com.sequenceiq.it.cloudbreak.action.v4.database;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DatabaseListAction implements Action<DatabaseTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseListAction.class);

    @Override
    public DatabaseTestDto action(TestContext testContext, DatabaseTestDto testDto, CloudbreakClient client) throws Exception {
        Collection<DatabaseV4Response> responses = client.getCloudbreakClient()
                .databaseV4Endpoint()
                .list(client.getWorkspaceId(), null, Boolean.TRUE)
                .getResponses();
        testDto.setResponses(responses.stream().collect(Collectors.toSet()));
        Log.whenJson(LOGGER, " Database listed successfully:\n", testDto.getResponses());
        return testDto;
    }
}
