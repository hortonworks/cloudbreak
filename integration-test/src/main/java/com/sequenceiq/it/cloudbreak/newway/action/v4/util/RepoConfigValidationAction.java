package com.sequenceiq.it.cloudbreak.newway.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.util.RepoConfigValidationTestDto;

public class RepoConfigValidationAction implements Action<RepoConfigValidationTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoConfigValidationAction.class);

    @Override
    public RepoConfigValidationTestDto action(TestContext testContext, RepoConfigValidationTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Posting repository config for validation";
        LOGGER.info("{}", logInitMessage);
        testDto.setResponse(cloudbreakClient.getCloudbreakClient().utilV4Endpoint().repositoryConfigValidationRequest(testDto.getRequest()));
        LOGGER.info("{} was successful", logInitMessage);
        return testDto;
    }
}
