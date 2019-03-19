package com.sequenceiq.it.cloudbreak.newway.action.v4.clustertemplate;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.clustertemplate.ClusterTemplateTestDto;

public class ClusterTemplateCreateAction implements Action<ClusterTemplateTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateCreateAction.class);

    @Override
    public ClusterTemplateTestDto action(TestContext testContext, ClusterTemplateTestDto testDto, CloudbreakClient client) throws Exception {
        log(LOGGER, "ClusterTemplateEntity name: " + testDto.getName());
        logJSON(LOGGER, " ClusterTemplateEntity post request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .clusterTemplateV4EndPoint()
                        .post(client.getWorkspaceId(), testDto.getRequest()));
        logJSON(LOGGER, " ClusterTemplateEntity created  successfully:\n", testDto.getResponse());
        log(LOGGER, "ClusterTemplateEntity ID: " + testDto.getResponse().getId());

        return testDto;
    }
}
