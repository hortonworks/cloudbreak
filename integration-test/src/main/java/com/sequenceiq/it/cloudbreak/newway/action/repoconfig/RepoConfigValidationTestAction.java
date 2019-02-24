package com.sequenceiq.it.cloudbreak.newway.action.repoconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.repoconfig.RepoConfigValidationTestDto;

public class RepoConfigValidationTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoConfigValidationTestAction.class);

    private RepoConfigValidationTestAction() {
    }

    public static RepoConfigValidationTestDto postRepositoryConfigValidation(TestContext testContext, RepoConfigValidationTestDto entity,
            CloudbreakClient client) {
        String logInitMessage = "Posting repository config for validation";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(client.getCloudbreakClient().utilV4Endpoint().repositoryConfigValidationRequest(entity.getRequest()));
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
