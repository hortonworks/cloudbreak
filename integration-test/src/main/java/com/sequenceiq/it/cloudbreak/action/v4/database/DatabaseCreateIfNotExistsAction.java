package com.sequenceiq.it.cloudbreak.action.v4.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DatabaseCreateIfNotExistsAction implements Action<DatabaseTestDto> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCreateIfNotExistsAction.class);

    @Override
    public DatabaseTestDto action(TestContext testContext, DatabaseTestDto testDto, CloudbreakClient client) throws Exception {
        LOGGER.info("Create Database with name: {}", testDto.getRequest().getName());
        try {
            testDto.setResponse(
                    client.getCloudbreakClient().databaseV4Endpoint().create(client.getWorkspaceId(), testDto.getRequest())
            );
            Log.logJSON(LOGGER, "Database created successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.info("Cannot create Database, fetch existed one: {}", testDto.getRequest().getName());
            testDto.setResponse(
                    client.getCloudbreakClient().databaseV4Endpoint()
                            .get(client.getWorkspaceId(), testDto.getRequest().getName()));
        }
        if (testDto.getResponse() == null) {
            throw new IllegalStateException("Database could not be created.");
        }
        return testDto;
    }
}
