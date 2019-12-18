package com.sequenceiq.it.cloudbreak.action.v4.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DatabaseCreateIfNotExistsAction implements Action<DatabaseTestDto, CloudbreakClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCreateIfNotExistsAction.class);

    @Override
    public DatabaseTestDto action(TestContext testContext, DatabaseTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, " Database create request:\n", testDto.getRequest());
        try {
            testDto.setResponse(
                    client.getCloudbreakClient().databaseV4Endpoint().create(client.getWorkspaceId(), testDto.getRequest())
            );
            Log.whenJson(LOGGER, "Database created successfully: ", testDto.getResponse());
        } catch (Exception e) {
            Log.when(LOGGER, "Cannot create Database, fetch existed one: " + testDto.getRequest().getName());
            testDto.setResponse(
                    client.getCloudbreakClient().databaseV4Endpoint()
                            .get(client.getWorkspaceId(), testDto.getRequest().getName()));
            Log.whenJson(LOGGER, "Database fetched successfully: ", testDto.getResponse());
        }
        if (testDto.getResponse() == null) {
            throw new IllegalStateException("Database could not be created.");
        }
        return testDto;
    }
}
