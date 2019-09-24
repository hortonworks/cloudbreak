package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class StackStopAction implements ActionV2<StackEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopAction.class);

    @Override
    public StackEntity action(TestContext testContext, StackEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, " Name: " + entity.getRequest().getGeneral().getName());
        logJSON(LOGGER, " Stack stop request:\n", entity.getRequest());
        Response response = client.getCloudbreakClient()
                .stackV3Endpoint().putStopInWorkspace(client.getWorkspaceId(), entity.getName());
        logJSON(LOGGER, " Stack stop request accepted:\n", response.getStatus());
        log(LOGGER, " ID: " + entity.getResponse().getId());

        return entity;
    }
}
