package com.sequenceiq.it.cloudbreak.newway.action.database;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseEntity;

public class DatabaseListAction implements Action<DatabaseEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseListAction.class);

    @Override
    public DatabaseEntity action(TestContext testContext, DatabaseEntity entity, CloudbreakClient client) throws Exception {
        Collection<DatabaseV4Response> responses = client.getCloudbreakClient()
                .databaseV4Endpoint()
                .list(client.getWorkspaceId(), null, Boolean.TRUE)
                .getResponses();
        entity.setResponses(responses.stream().collect(Collectors.toSet()));
        logJSON(LOGGER, " Database listed successfully:\n", entity.getResponses());
        return entity;
    }
}
