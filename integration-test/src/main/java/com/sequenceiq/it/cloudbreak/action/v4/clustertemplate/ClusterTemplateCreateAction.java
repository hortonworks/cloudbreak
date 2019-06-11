package com.sequenceiq.it.cloudbreak.action.v4.clustertemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class ClusterTemplateCreateAction implements Action<ClusterTemplateTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateCreateAction.class);

    @Override
    public ClusterTemplateTestDto action(TestContext testContext, ClusterTemplateTestDto testDto, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, "ClusterTemplateEntity name: " + testDto.getName());
        Log.logJSON(LOGGER, " ClusterTemplateEntity post request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .clusterTemplateV4EndPoint()
                        .post(client.getWorkspaceId(), testDto.getRequest()));
        Log.logJSON(LOGGER, " ClusterTemplateEntity created  successfully:\n", testDto.getResponse());
        Log.log(LOGGER, "ClusterTemplateEntity ID: " + testDto.getResponse().getId());

        return testDto;
    }
}
