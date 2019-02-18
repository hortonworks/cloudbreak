package com.sequenceiq.it.cloudbreak.newway.action.database;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseEntity;

public class DatabaseCreateAction implements Action<DatabaseEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCreateAction.class);

    @Override
    public DatabaseEntity action(TestContext testContext, DatabaseEntity entity, CloudbreakClient client) throws Exception {
        logJSON(LOGGER, " Database create request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .databaseV4Endpoint()
                        .create(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, " Database created successfully:\n", entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }

}