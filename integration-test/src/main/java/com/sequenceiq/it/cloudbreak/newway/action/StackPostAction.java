package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class StackPostAction implements ActionV2<StackEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackPostAction.class);

    private static final String SUBNET_ID_KEY = "subnetId";

    private static final String NETWORK_ID_KEY = "networkId";

    @Override
    public StackEntity action(TestContext testContext, StackEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, " Name: " + entity.getRequest().getGeneral().getName());
        logJSON(LOGGER, " Stack post request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .stackV3Endpoint()
                        .createInWorkspace(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, " Stack created  successfully:\n", entity.getResponse());
        log(LOGGER, " ID: " + entity.getResponse().getId());

        return entity;
    }
}
