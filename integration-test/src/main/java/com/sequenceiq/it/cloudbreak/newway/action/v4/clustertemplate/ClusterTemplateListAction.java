package com.sequenceiq.it.cloudbreak.newway.action.v4.clustertemplate;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.ClusterTemplateUtil;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.clustertemplate.ClusterTemplateTestDto;

public class ClusterTemplateListAction implements Action<ClusterTemplateTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateListAction.class);

    @Override
    public ClusterTemplateTestDto action(TestContext testContext, ClusterTemplateTestDto entity, CloudbreakClient client) throws Exception {
        logJSON(LOGGER, " ClusterTemplateEntity get request:\n", entity.getRequest());
        Collection<ClusterTemplateViewV4Response> responses = client.getCloudbreakClient()
                .clusterTemplateV4EndPoint()
                .list(client.getWorkspaceId()).getResponses();
        entity.setResponses(ClusterTemplateUtil.getResponseFromViews(responses));
        logJSON(LOGGER, " ClusterTemplateEntity list successfully:\n", responses);
        return entity;
    }
}
