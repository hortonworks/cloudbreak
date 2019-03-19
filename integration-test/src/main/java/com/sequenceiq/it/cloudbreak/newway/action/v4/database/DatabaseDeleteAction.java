package com.sequenceiq.it.cloudbreak.newway.action.v4.database;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.database.DatabaseTestDto;

public class DatabaseDeleteAction implements Action<DatabaseTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseDeleteAction.class);

    @Override
    public DatabaseTestDto action(TestContext testContext, DatabaseTestDto testDto, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        logJSON(LOGGER, " Database delete request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .databaseV4Endpoint()
                        .delete(client.getWorkspaceId(), testDto.getName()));
        logJSON(LOGGER, " Database deleted successfully:\n", testDto.getResponse());
        return testDto;
    }

}