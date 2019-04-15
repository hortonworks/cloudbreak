package com.sequenceiq.it.cloudbreak.newway.action.v4.clustertemplate;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTemplateTestDto;

public class LaunchClusterFromClusterTemplateAction implements Action<ClusterTemplateTestDto> {

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
        logJSON(LOGGER, "Stack from template post request:\n", testDto.getRequest().getStackTemplate());
        StackTemplateTestDto stackEntity = testContext.get(stackTemplateKey);
        stackEntity.setResponse(client.getCloudbreakClient()
                .stackV4Endpoint()
                .post(client.getWorkspaceId(), stackEntity.getRequest()));
        logJSON(LOGGER, " Stack from template created  successfully:\n", testDto.getResponse());
        log(LOGGER, "Stack from template ID: " + testDto.getResponse().getId());
        return testDto;
    }
}
