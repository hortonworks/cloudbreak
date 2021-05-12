package com.sequenceiq.environment.experience.common;

import org.springframework.stereotype.Component;

@Component
public class CommonExperiencePathCreator {

    public String createPathToExperience(CommonExperience xp) {
        return createCombinedBasedAddress(xp) + xp.getInternalEnvironmentEndpoint();
    }

    public String createPathToExperiencePolicyProvider(CommonExperience xp) {
        return createCombinedBasedAddress(xp) + xp.getPolicyEndpoint();
    }

    private String createCombinedBasedAddress(CommonExperience xp) {
        return xp.getBaseAddress() + ":" + xp.getEnvironmentEndpointPort();
    }

}
