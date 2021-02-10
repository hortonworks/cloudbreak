package com.sequenceiq.environment.experience.common;

import org.springframework.stereotype.Component;

@Component
public class CommonExperiencePathCreator {

    public String createPathToExperience(CommonExperience xp) {
        return xp.getAddress() + xp.getInternalEnvironmentEndpoint();
    }

}
