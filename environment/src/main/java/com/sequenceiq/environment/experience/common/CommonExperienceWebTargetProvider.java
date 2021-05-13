package com.sequenceiq.environment.experience.common;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.throwIfTrue;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;

import java.util.Map;

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

    private final Map<String, String> componentsToReplace;

    private final Client client;

    CommonExperienceWebTargetProvider(@Value("${environment.experience.path.componentToReplace}") Map<String, String> componentsToReplace, Client client) {
        throwIfTrue(componentsToReplace == null || componentsToReplace.isEmpty(),
                () -> new IllegalArgumentException("Component what should be replaced in experience path must not be empty or null."));
        this.componentsToReplace = componentsToReplace;
        this.client = client;
    }

    WebTarget createWebTargetForClusterFetch(String experienceBasePath, String environmentCrn) {
        checkExperienceBasePath(experienceBasePath);
        LOGGER.debug("Creating WebTarget to connect experience for cluster fetch");
        return client.target(experienceBasePath.replace(componentsToReplace.get("envCrn"), environmentCrn));
    }

    WebTarget createWebTargetForPolicyFetch(String experienceBasePath, String cloudProvider) {
        checkExperienceBasePath(experienceBasePath);
        LOGGER.debug("Creating WebTarget to connect experience for cluster fetch");
        return client.target(experienceBasePath.replace(componentsToReplace.get("cloudProvider"), cloudProvider));
    }

    private void checkExperienceBasePath(String experienceBasePath) {
        throwIfNull(experienceBasePath, () -> new IllegalArgumentException(INVALID_XP_BASE_PATH_GIVEN_MSG));
    }

}
