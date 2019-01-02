package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackTemplateEntity;

public class DeleteClusterFromTemplateAction implements Action<ClusterTemplateEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteClusterFromTemplateAction.class);

    private String stackTemplateKey;

    public DeleteClusterFromTemplateAction(String stackTemplateKey) {
        this.stackTemplateKey = stackTemplateKey;
    }

    @Override
    public ClusterTemplateEntity action(TestContext testContext, ClusterTemplateEntity entity, CloudbreakClient client) throws Exception {
        if (entity.getResponse() == null)  {
            logJSON(LOGGER, "Cluster response is null", entity.getRequest().getStackTemplate());
            return entity;
        }
        logJSON(LOGGER, "Stack from template post request:\n", entity.getRequest().getStackTemplate());
        StackTemplateEntity stackEntity = testContext.get(stackTemplateKey);
        client.getCloudbreakClient()
                .stackV4Endpoint()
                .delete(client.getWorkspaceId(), stackEntity.getResponse().getName(), false, null);
        logJSON(LOGGER, " Stack from template created  successfully:\n", entity.getResponse());
        log(LOGGER, "Stack from template ID: " + entity.getResponse().getId());
        return entity;
    }
}
