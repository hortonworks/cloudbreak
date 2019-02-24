package com.sequenceiq.it.cloudbreak.newway.action.deploymentpref;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.deploymentpref.DeploymentPreferencesTestDto;

public class DeploymentPreferencesTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentPreferencesTestAction.class);

    private DeploymentPreferencesTestAction() {
    }

    public static DeploymentPreferencesTestDto getDeployment(TestContext testContext, DeploymentPreferencesTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining deployment";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(client.getCloudbreakClient().utilV4Endpoint().deployment());
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
