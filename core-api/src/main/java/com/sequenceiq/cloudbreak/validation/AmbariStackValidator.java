package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.Versioned;

public class AmbariStackValidator implements ConstraintValidator<ValidAmbariStack, AmbariStackDetailsJson> {

    public static final String MIN_HDP_VERSION = "2.5";

    @Override
    public void initialize(ValidAmbariStack constraintAnnotation) {
    }

    @Override
    public boolean isValid(AmbariStackDetailsJson ambariStackDetailsJson, ConstraintValidatorContext context) {
        if ("HDP".equalsIgnoreCase(ambariStackDetailsJson.getStack())) {
            if (StringUtils.isBlank(ambariStackDetailsJson.getVersion())) {
                return false;
            }

            Versioned hdpVersion = () -> ambariStackDetailsJson.getVersion().length() > MIN_HDP_VERSION.length()
                    ? ambariStackDetailsJson.getVersion().substring(0, MIN_HDP_VERSION.length()) : ambariStackDetailsJson.getVersion();
            Versioned minVersion = () -> MIN_HDP_VERSION;
            int compared = new VersionComparator().compare(minVersion, hdpVersion);
            return compared <= 0;
        }
        return true;
    }
}
