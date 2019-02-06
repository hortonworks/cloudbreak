package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;

public class ClusterTemplateV4CreateAction implements Action<ClusterTemplateEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateV4CreateAction.class);

    @Override
    public ClusterTemplateEntity action(TestContext testContext, ClusterTemplateEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, "ClusterTemplateEntity name: " + entity.getName());
        logJSON(LOGGER, " ClusterTemplateEntity post request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .clusterTemplateV4EndPoint()
                        .post(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, " ClusterTemplateEntity created  successfully:\n", entity.getResponse());
        log(LOGGER, "ClusterTemplateEntity ID: " + entity.getResponse().getId());

        return entity;
    }
}
