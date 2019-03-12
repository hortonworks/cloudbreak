package com.sequenceiq.it.cloudbreak.newway.action.database;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseEntity;

public class DatabaseCreateIfNotExistsAction implements Action<DatabaseEntity> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCreateIfNotExistsAction.class);

    @Override
    public DatabaseEntity action(TestContext testContext, DatabaseEntity entity, CloudbreakClient client) {
        LOGGER.info("Create Database with name: {}", entity.getRequest().getName());
        try {
            entity.setResponse(
                    client.getCloudbreakClient().databaseV4Endpoint().create(client.getWorkspaceId(), entity.getRequest())
            );
            logJSON(LOGGER, "Database created successfully: ", entity.getRequest());
        } catch (Exception e) {
            LOGGER.info("Cannot create Database, fetch existed one: {}", entity.getRequest().getName());
            entity.setResponse(
                    client.getCloudbreakClient().databaseV4Endpoint()
                            .get(client.getWorkspaceId(), entity.getRequest().getName()));
        }
        if (entity.getResponse() == null) {
            throw new IllegalStateException("Database could not be created.");
        }
        return entity;
    }
}
