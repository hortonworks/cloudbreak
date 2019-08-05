package com.sequenceiq.it.cloudbreak.action.v4.clustertemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class ClusterTemplateGetAction implements Action<ClusterTemplateTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateGetAction.class);

    @Override
    public ClusterTemplateTestDto action(TestContext testContext, ClusterTemplateTestDto testDto, CloudbreakClient client) throws Exception {
        Log.logJSON(LOGGER, " ClusterTemplateEntity get request:\n", testDto.getRequest());
        ClusterTemplateV4Response response = client.getCloudbreakClient()
                .clusterTemplateV4EndPoint()
                .getByName(client.getWorkspaceId(), testDto.getName());
        testDto.setResponses(Sets.newHashSet(response));
        Log.logJSON(LOGGER, " ClusterTemplateEntity get call was successful:\n", response);
        return testDto;
    }
}
