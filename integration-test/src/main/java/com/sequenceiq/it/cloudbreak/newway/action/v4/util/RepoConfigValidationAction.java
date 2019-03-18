package com.sequenceiq.it.cloudbreak.newway.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.util.RepoConfigValidationTestDto;

public class RepoConfigValidationAction implements Action<RepoConfigValidationTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoConfigValidationAction.class);

    @Override
    public RepoConfigValidationTestDto action(TestContext testContext, RepoConfigValidationTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Posting repository config for validation";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(cloudbreakClient.getCloudbreakClient().utilV4Endpoint().repositoryConfigValidationRequest(entity.getRequest()));
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }
}
