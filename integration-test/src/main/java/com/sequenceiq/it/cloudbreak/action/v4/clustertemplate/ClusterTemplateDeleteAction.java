package com.sequenceiq.it.cloudbreak.action.v4.clustertemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class ClusterTemplateDeleteAction implements Action<ClusterTemplateTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateDeleteAction.class);

    @Override
    public ClusterTemplateTestDto action(TestContext testContext, ClusterTemplateTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, "ClusterTemplateEntity delete, name: " + testDto.getRequest().getName());
        client.getDefaultClient(testContext)
                .clusterTemplateV4EndPoint()
                .deleteByName(client.getWorkspaceId(), testDto.getRequest().getName());
        Log.when(LOGGER, "ClusterTemplateEntity deleted successfully: " + testDto.getResponse().getId());

        return testDto;
    }
}
