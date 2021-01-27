package com.sequenceiq.environment.experience.common;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.throwIfTrue;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class CommonExperienceWebTargetProvider {

    private static final String INVALID_XP_BASE_PATH_GIVEN_MSG = "Experience base path should not be null!";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonExperienceWebTargetProvider.class);

    private final String componentToReplaceInPath;

    private final Client client;

    CommonExperienceWebTargetProvider(@Value("${experience.scan.path.componentToReplace}") String componentToReplaceInPath, Client client) {
        throwIfTrue(isEmpty(componentToReplaceInPath),
                () -> new IllegalArgumentException("Component what should be replaced in experience path must not be empty or null."));
        this.componentToReplaceInPath = componentToReplaceInPath;
        this.client = client;
    }

    WebTarget createWebTargetBasedOnInputs(String experienceBasePath, String environmentCrn) {
        LOGGER.debug("Creating WebTarget to connect experience");
        return client.target(createPathToExperience(experienceBasePath, environmentCrn));
    }

    private String createPathToExperience(String experienceBasePath, String environmentCrn) {
        checkExperienceBasePath(experienceBasePath);
        return experienceBasePath.replace(componentToReplaceInPath, environmentCrn);
    }

    private void checkExperienceBasePath(String experienceBasePath) {
        throwIfNull(experienceBasePath, () -> new IllegalArgumentException(INVALID_XP_BASE_PATH_GIVEN_MSG));
    }

}
