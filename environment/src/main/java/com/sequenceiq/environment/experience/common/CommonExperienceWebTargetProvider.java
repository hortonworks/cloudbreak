package com.sequenceiq.environment.experience.common;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;

import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.experience.config.ExperiencePathConfig;
import com.sequenceiq.environment.experience.config.ExperiencePathPlaceholders;

@Component
class CommonExperienceWebTargetProvider {

    private static final String INVALID_XP_BASE_PATH_GIVEN_MSG = "Experience base path should not be null!";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonExperienceWebTargetProvider.class);

    private final Map<String, String> componentsToReplace;

    private final Client client;

    CommonExperienceWebTargetProvider(ExperiencePathConfig componentsToReplace, Client client) {
        this.componentsToReplace = componentsToReplace.getToReplace();
        this.client = client;
    }

    WebTarget createWebTargetForClusterFetch(String experienceBasePath, String environmentCrn) {
        checkExperienceBasePath(experienceBasePath);
        LOGGER.debug("Creating WebTarget to connect experience for cluster fetch");
        return client.target(experienceBasePath.replace(componentsToReplace.get(ExperiencePathPlaceholders.ENVIRONMENT_CRN.getPlaceholder()), environmentCrn));
    }

    WebTarget createWebTargetForPolicyFetch(String experienceBasePath, String cloudProvider) {
        checkExperienceBasePath(experienceBasePath);
        String path = experienceBasePath.replace(componentsToReplace.get(ExperiencePathPlaceholders.CLOUD_PROVIDER.getPlaceholder()), cloudProvider);
        LOGGER.debug("Creating WebTarget to connect an experience on path [{}] fetch", path);
        return client.target(path);
    }

    private void checkExperienceBasePath(String experienceBasePath) {
        throwIfNull(experienceBasePath, () -> new IllegalArgumentException(INVALID_XP_BASE_PATH_GIVEN_MSG));
    }

}
