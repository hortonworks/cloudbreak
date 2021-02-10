package com.sequenceiq.environment.experience.common;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class CommonExperienceValidator {

    private static final String ENV_VAR_PATTERN_REGEX = "\\$\\{.*}";

    public boolean isExperienceFilled(CommonExperience xp) {
        return xp != null && isGivenExperienceValuesAreFilled(xp.getInternalEnvironmentEndpoint(), xp.getAddress());
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
