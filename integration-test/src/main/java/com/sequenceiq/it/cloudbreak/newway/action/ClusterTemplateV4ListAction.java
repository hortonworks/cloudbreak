package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.ClusterTemplateUtil;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;

public class ClusterTemplateV4ListAction implements ActionV2<ClusterTemplateEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateV4ListAction.class);

    @Override
    public ClusterTemplateEntity action(TestContext testContext, ClusterTemplateEntity entity, CloudbreakClient client) throws Exception {
        logJSON(LOGGER, " ClusterTemplateEntity get request:\n", entity.getRequest());
        Set<ClusterTemplateViewV4Response> responses = client.getCloudbreakClient()
                .clusterTemplateV4EndPoint()
                .list(client.getWorkspaceId()).getResponses();
        entity.setResponses(ClusterTemplateUtil.getResponseFromViews(responses));
        logJSON(LOGGER, " ClusterTemplateEntity list successfully:\n", responses);
        return entity;
    }
}
