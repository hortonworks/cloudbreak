package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.Versioned;

public class AmbariStackValidator implements ConstraintValidator<ValidAmbariStack, StackRepositoryV4Request> {

    private static final String MIN_HDP_VERSION = "2.7";

    @Override
    public void initialize(ValidAmbariStack constraintAnnotation) {

    }

    @Override
    public boolean isValid(StackRepositoryV4Request ambariStackDetailsJson, ConstraintValidatorContext context) {
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
