package com.sequenceiq.it.cloudbreak.action.v4.clustertemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTemplateTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class LaunchClusterFromClusterTemplateAction implements Action<ClusterTemplateTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchClusterFromClusterTemplateAction.class);

    private String stackTemplateKey;

    public LaunchClusterFromClusterTemplateAction(String stackTemplateKey) {
        this.stackTemplateKey = stackTemplateKey;
    }

    public LaunchClusterFromClusterTemplateAction(Class<StackTemplateTestDto> stackTemplateKey) {
        this.stackTemplateKey = stackTemplateKey.getSimpleName();
    }

    @Override
    public ClusterTemplateTestDto action(TestContext testContext, ClusterTemplateTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, "Stack from template post request:\n", testDto.getRequest());
        StackTemplateTestDto stackEntity = testContext.get(stackTemplateKey);
        stackEntity.setResponse(client.getCloudbreakClient()
                .stackV4Endpoint()
                .post(client.getWorkspaceId(), stackEntity.getRequest()));
        Log.whenJson(LOGGER, " Stack from template created  successfully:\n", testDto.getResponse());
        return testDto;
    }
}
