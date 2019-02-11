package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;

public class ClusterTemplateV4DeleteAction implements Action<ClusterTemplateEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateV4DeleteAction.class);

    @Override
    public ClusterTemplateEntity action(TestContext testContext, ClusterTemplateEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, "ClusterTemplateEntity delete, name: " + entity.getRequest().getName());
        client.getCloudbreakClient()
                .clusterTemplateV4EndPoint()
                .delete(client.getWorkspaceId(), entity.getRequest().getName());
        log(LOGGER, "ClusterTemplateEntity deleted successfully: " + entity.getResponse().getId());

        return entity;
    }
}
