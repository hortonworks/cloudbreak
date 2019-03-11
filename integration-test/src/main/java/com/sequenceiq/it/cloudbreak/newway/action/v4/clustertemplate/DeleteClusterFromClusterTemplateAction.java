package com.sequenceiq.it.cloudbreak.newway.action.v4.clustertemplate;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.StackTemplateEntity;

public class DeleteClusterFromClusterTemplateAction implements Action<ClusterTemplateTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteClusterFromClusterTemplateAction.class);

    private String stackTemplateKey;

    public DeleteClusterFromClusterTemplateAction(String stackTemplateKey) {
        this.stackTemplateKey = stackTemplateKey;
    }

    @Override
    public ClusterTemplateTestDto action(TestContext testContext, ClusterTemplateTestDto entity, CloudbreakClient client) throws Exception {
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
