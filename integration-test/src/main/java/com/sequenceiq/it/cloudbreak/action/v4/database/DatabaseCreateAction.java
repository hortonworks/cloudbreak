package com.sequenceiq.it.cloudbreak.action.v4.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DatabaseCreateAction implements Action<DatabaseTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCreateAction.class);

    @Override
    public DatabaseTestDto action(TestContext testContext, DatabaseTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, " Database create request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .databaseV4Endpoint()
                        .create(client.getWorkspaceId(), testDto.getRequest()));
        Log.whenJson(LOGGER, " Database created successfully:\n", testDto.getResponse());
        Log.when(LOGGER, String.format(" ID: %s", testDto.getResponse().getId()));

        return testDto;
    }

}