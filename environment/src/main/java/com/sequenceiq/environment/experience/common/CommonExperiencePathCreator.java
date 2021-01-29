package com.sequenceiq.environment.experience.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CommonExperiencePathCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonExperiencePathCreator.class);

    private final String experienceProtocol;

    public CommonExperiencePathCreator(@Value("${experience.scan.protocol:https}") String experienceProtocol) {
        this.experienceProtocol = experienceProtocol;
        LOGGER.debug("Experience connection protocol set to: {}", this.experienceProtocol);
    }

    public String createPathToExperience(CommonExperience xp) {
        return experienceProtocol + "://" + xp.getHostAddress() + ":" + xp.getPort() + xp.getInternalEnvEndpoint();
    }

}
