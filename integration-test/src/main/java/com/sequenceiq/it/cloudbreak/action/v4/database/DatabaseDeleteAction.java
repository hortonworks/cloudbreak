package com.sequenceiq.it.cloudbreak.action.v4.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DatabaseDeleteAction implements Action<DatabaseTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseDeleteAction.class);

    @Override
    public DatabaseTestDto action(TestContext testContext, DatabaseTestDto testDto, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, String.format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, " Database delete request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .databaseV4Endpoint()
                        .delete(client.getWorkspaceId(), testDto.getName()));
        Log.logJSON(LOGGER, " Database deleted successfully:\n", testDto.getResponse());
        return testDto;
    }

}