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

public class DatabaseCreateAction implements Action<DatabaseTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCreateAction.class);

    @Override
    public DatabaseTestDto action(TestContext testContext, DatabaseTestDto testDto, CloudbreakClient client) throws Exception {
        logJSON(LOGGER, " Database create request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .databaseV4Endpoint()
                        .create(client.getWorkspaceId(), testDto.getRequest()));
        logJSON(LOGGER, " Database created successfully:\n", testDto.getResponse());
        log(LOGGER, format(" ID: %s", testDto.getResponse().getId()));

        return testDto;
    }

}