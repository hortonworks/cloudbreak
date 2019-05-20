package com.sequenceiq.it.cloudbreak.action.v4.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DatabaseTestConnectionAction implements Action<DatabaseTestTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTestConnectionAction.class);

    @Override
    public DatabaseTestTestDto action(TestContext testContext, DatabaseTestTestDto testDto, CloudbreakClient client) throws Exception {
        Log.logJSON(LOGGER, " Database test connection request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .databaseV4Endpoint()
                        .test(client.getWorkspaceId(), testDto.getRequest()));
        Log.logJSON(LOGGER, " Database test connection executed successfully:\n", testDto.getResponse());

        return testDto;
    }

}