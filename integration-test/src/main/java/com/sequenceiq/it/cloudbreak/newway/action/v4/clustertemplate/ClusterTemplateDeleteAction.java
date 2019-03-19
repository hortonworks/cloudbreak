package com.sequenceiq.it.cloudbreak.newway.action.v4.clustertemplate;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.clustertemplate.ClusterTemplateTestDto;

public class ClusterTemplateDeleteAction implements Action<ClusterTemplateTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateDeleteAction.class);

    @Override
    public ClusterTemplateTestDto action(TestContext testContext, ClusterTemplateTestDto testDto, CloudbreakClient client) throws Exception {
        log(LOGGER, "ClusterTemplateEntity delete, name: " + testDto.getRequest().getName());
        client.getCloudbreakClient()
                .clusterTemplateV4EndPoint()
                .delete(client.getWorkspaceId(), testDto.getRequest().getName());
        log(LOGGER, "ClusterTemplateEntity deleted successfully: " + testDto.getResponse().getId());

        return testDto;
    }
}
