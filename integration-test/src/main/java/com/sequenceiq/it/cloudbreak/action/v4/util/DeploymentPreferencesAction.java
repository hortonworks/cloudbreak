package com.sequenceiq.it.cloudbreak.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.DeploymentPreferencesTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DeploymentPreferencesAction implements Action<DeploymentPreferencesTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentPreferencesAction.class);

    @Override
    public DeploymentPreferencesTestDto action(TestContext testContext, DeploymentPreferencesTestDto testDto, CloudbreakClient client) throws Exception {
        testDto.setResponse(client.getDefaultClient(testContext).utilV4Endpoint().deployment());
        Log.whenJson(LOGGER, "Deployment preferences response:\n", testDto.getResponse());
        return testDto;
    }
}
