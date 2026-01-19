package com.sequenceiq.it.cloudbreak.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.RepoConfigValidationTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class RepoConfigValidationAction implements Action<RepoConfigValidationTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoConfigValidationAction.class);

    @Override
    public RepoConfigValidationTestDto action(TestContext testContext, RepoConfigValidationTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Log.whenJson(LOGGER, "Posting repository config request:\n", testDto.getRequest());
        testDto.setResponse(cloudbreakClient.getDefaultClient(testContext).utilV4Endpoint().repositoryConfigValidationRequest(testDto.getRequest()));
        Log.whenJson(LOGGER, "Posting repository config response:\n", testDto.getResponse());

        return testDto;
    }
}
