package com.sequenceiq.it.cloudbreak.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.DeploymentPreferencesTestDto;

public class DeploymentPreferencesAction implements Action<DeploymentPreferencesTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentPreferencesAction.class);

    @Override
    public DeploymentPreferencesTestDto action(TestContext testContext, DeploymentPreferencesTestDto testDto, CloudbreakClient client) throws Exception {
        String logInitMessage = "Obtaining deployment";
        LOGGER.info("{}", logInitMessage);
        testDto.setResponse(client.getCloudbreakClient().utilV4Endpoint().deployment());
        LOGGER.info("{} was successful", logInitMessage);
        return testDto;
    }
}
