package com.sequenceiq.it.cloudbreak.action.v4.clustertemplate;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ClusterTemplateUtil;

public class ClusterTemplateListAction implements Action<ClusterTemplateTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateListAction.class);

    @Override
    public ClusterTemplateTestDto action(TestContext testContext, ClusterTemplateTestDto testDto, CloudbreakClient client) throws Exception {
        Log.logJSON(LOGGER, " ClusterTemplateEntity get request:\n", testDto.getRequest());
        Collection<ClusterTemplateViewV4Response> responses = client.getCloudbreakClient()
                .clusterTemplateV4EndPoint()
                .list(client.getWorkspaceId()).getResponses();
        testDto.setResponses(ClusterTemplateUtil.getResponseFromViews(responses));
        Log.logJSON(LOGGER, " ClusterTemplateEntity list successfully:\n", responses);
        return testDto;
    }
}
