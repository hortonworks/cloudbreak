package com.sequenceiq.environment.experience.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CommonExperiencePathCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonExperiencePathCreator.class);

    public String createPathToExperience(CommonExperience xp) {
        String path = createCombinedBasedAddress(xp) + xp.getInternalEnvironmentEndpoint();
        LOGGER.info("Path created to experience: {}", path);
        return path;
    }

    public String createPathToExperiencePolicyProvider(CommonExperience xp) {
        String path = createCombinedBasedAddress(xp) + xp.getPolicyEndpoint();
        LOGGER.info("Path created to experience for policy fetch: {}", path);
        return path;
    }

    private String createCombinedBasedAddress(CommonExperience xp) {
        return xp.getBaseAddress() + ":" + xp.getEnvironmentEndpointPort();
    }

}
