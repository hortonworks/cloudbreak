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

public class DatabaseDeleteAction implements Action<DatabaseEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseDeleteAction.class);

    @Override
    public DatabaseEntity action(TestContext testContext, DatabaseEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, " Database delete request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .databaseV4Endpoint()
                        .delete(client.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, " Database deleted successfully:\n", entity.getResponse());
        return entity;
    }

}