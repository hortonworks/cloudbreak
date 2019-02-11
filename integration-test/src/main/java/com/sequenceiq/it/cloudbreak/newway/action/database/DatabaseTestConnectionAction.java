package com.sequenceiq.it.cloudbreak.newway.action.database;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseTestEntity;

public class DatabaseTestConnectionAction implements Action<DatabaseTestEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTestConnectionAction.class);

    @Override
    public DatabaseTestEntity action(TestContext testContext, DatabaseTestEntity entity, CloudbreakClient client) throws Exception {
        logJSON(LOGGER, " Database test connection request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .databaseV4Endpoint()
                        .test(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, " Database test connection executed successfully:\n", entity.getResponse());

        return entity;
    }

}