package com.sequenceiq.it.cloudbreak.newway.action.v4.database;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.database.DatabaseTestTestDto;

public class DatabaseTestConnectionAction implements Action<DatabaseTestTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTestConnectionAction.class);

    @Override
    public DatabaseTestTestDto action(TestContext testContext, DatabaseTestTestDto testDto, CloudbreakClient client) throws Exception {
        logJSON(LOGGER, " Database test connection request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .databaseV4Endpoint()
                        .test(client.getWorkspaceId(), testDto.getRequest()));
        logJSON(LOGGER, " Database test connection executed successfully:\n", testDto.getResponse());

        return testDto;
    }

}