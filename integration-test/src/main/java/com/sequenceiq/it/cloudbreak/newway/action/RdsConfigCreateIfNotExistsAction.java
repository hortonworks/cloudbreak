package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.RdsConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class RdsConfigCreateIfNotExistsAction implements Action<RdsConfigEntity> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RdsConfigCreateIfNotExistsAction.class);

    @Override
    public RdsConfigEntity action(TestContext testContext, RdsConfigEntity entity, CloudbreakClient client) throws Exception {
        LOGGER.info("Create RdsConfig with name: {}", entity.getRequest().getName());
        try {
            entity.setResponse(
                    client.getCloudbreakClient().databaseV4Endpoint().create(client.getWorkspaceId(), entity.getRequest())
            );
            logJSON(LOGGER, "RdsConfig created successfully: ", entity.getRequest());
        } catch (Exception e) {
            LOGGER.info("Cannot create RdsConfig, fetch existed one: {}", entity.getRequest().getName());
            entity.setResponse(
                    client.getCloudbreakClient().databaseV4Endpoint()
                            .get(client.getWorkspaceId(), entity.getRequest().getName()));
        }
        if (entity.getResponse() == null) {
            throw new IllegalStateException("RdsConfigF could not be created.");
        }
        return entity;
    }
}
