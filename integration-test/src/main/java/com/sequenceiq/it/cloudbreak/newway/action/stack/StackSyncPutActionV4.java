package com.sequenceiq.it.cloudbreak.newway.action.stack;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class StackSyncPutActionV4 implements Action<StackEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackSyncPutActionV4.class);

    @Override
    public StackEntity action(TestContext testContext, StackEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, " Stack post request:\n", entity.getRequest());
        client.getCloudbreakClient().stackV4Endpoint().putSync(client.getWorkspaceId(), entity.getName());
        logJSON(LOGGER, " Stack sync was successful:\n", entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));
        return entity;
    }

}