package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.Versioned;

public class AmbariRepoValidator implements ConstraintValidator<ValidAmbariRepo, AmbariRepoDetailsJson> {

    public static final String MIN_AMBARI_VERSION = "2.6";

    @Override
    public void initialize(ValidAmbariRepo constraintAnnotation) {
    }

    @Override
    public boolean isValid(AmbariRepoDetailsJson ambariRepoDetailsJson, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(ambariRepoDetailsJson.getVersion())) {
            return false;
        }

        Versioned ambariVersion = () -> ambariRepoDetailsJson.getVersion().length() > MIN_AMBARI_VERSION.length()
                ? ambariRepoDetailsJson.getVersion().substring(0, MIN_AMBARI_VERSION.length()) : ambariRepoDetailsJson.getVersion();
        Versioned minVersion = () -> MIN_AMBARI_VERSION;
        int compared = new VersionComparator().compare(minVersion, ambariVersion);
        return compared <= 0;
    }
}
