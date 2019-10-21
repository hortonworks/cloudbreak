package com.sequenceiq.environment.environment.experience.service;

import com.sequenceiq.environment.environment.experience.resolve.Experience;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class ExperienceValidator {

    private static final String ENV_VAR_PATTERN_REGEX = "\\$\\{.*}";

    public boolean isExperienceFilled(Experience xp) {
        return xp != null && isGivenExperienceValuesAreFilled(xp.getPathInfix(), xp.getPathPrefix(), xp.getPort());
    }

    private boolean isGivenExperienceValuesAreFilled(String... valuesToCheck) {
        for (String value : valuesToCheck) {
            boolean valid = false;
            if (StringUtils.isNotEmpty(value) && !value.matches(ENV_VAR_PATTERN_REGEX)) {
                valid = true;
            }
            if (!valid) {
                return false;
            }
        }
        return true;
    }

}
