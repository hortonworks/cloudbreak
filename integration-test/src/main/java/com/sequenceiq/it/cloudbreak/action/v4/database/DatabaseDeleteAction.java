package com.sequenceiq.it.cloudbreak.action.v4.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DatabaseDeleteAction implements Action<DatabaseTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseDeleteAction.class);

    @Override
    public DatabaseTestDto action(TestContext testContext, DatabaseTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, " Database delete request: " + testDto.getName());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .databaseV4Endpoint()
                        .delete(client.getWorkspaceId(), testDto.getName()));
        Log.whenJson(LOGGER, " Database deleted successfully:\n", testDto.getResponse());
        return testDto;
    }

}