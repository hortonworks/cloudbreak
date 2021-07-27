package com.sequenceiq.environment.experience.common;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.experience.config.ExperiencePathConfig;
import com.sequenceiq.environment.experience.config.ExperiencePathPlaceholders;

@Component
public class CommonExperiencePathCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonExperiencePathCreator.class);

    @Inject
    private ExperiencePathConfig pathConfig;

    private Map<String, String> componentsToReplace;

    @PostConstruct
    public void setUp() {
        componentsToReplace = pathConfig.getToReplace();
    }

    public String createPathToExperience(CommonExperience xp) {
        String path = createCombinedBasedAddress(xp) + xp.getInternalEnvironmentEndpoint();
        LOGGER.info("Path created to experience: {}", path);
        return path;
    }

    public String createPathToExperiencePolicyProvider(CommonExperience xp) {
        String path = createCombinedBasedAddress(xp)
                + getTruncatedInternalEnvironmentEndpoint(xp)
                + xp.getPolicyEndpoint();
        LOGGER.info("Path created to experience for policy fetch: {}", path);
        return path;
    }

    private String createCombinedBasedAddress(CommonExperience xp) {
        return xp.getBaseAddress() + ":" + xp.getEnvironmentEndpointPort();
    }

    private String getTruncatedInternalEnvironmentEndpoint(CommonExperience xp) {
        return xp.getInternalEnvironmentEndpoint().replace("/" + componentsToReplace.get(ExperiencePathPlaceholders.ENVIRONMENT_CRN.getPlaceholder()), "");
    }

}
