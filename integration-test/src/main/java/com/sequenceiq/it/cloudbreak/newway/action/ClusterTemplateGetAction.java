package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cluster_template.responses.ClusterTemplateV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;

public class ClusterTemplateGetAction implements ActionV2<ClusterTemplateEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateGetAction.class);

    @Override
    public ClusterTemplateEntity action(TestContext testContext, ClusterTemplateEntity entity, CloudbreakClient client) throws Exception {
        logJSON(LOGGER, " ClusterTemplateEntity get request:\n", entity.getRequest());
        ClusterTemplateV4Response response = client.getCloudbreakClient()
                .clusterTemplateV4EndPoint()
                .get(client.getWorkspaceId(), entity.getName());
        entity.setResponses(Sets.newHashSet(response));
        logJSON(LOGGER, " ClusterTemplateEntity get call was successful:\n", response);
        return entity;
    }
}
