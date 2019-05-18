package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.ambarirepository.AmbariRepositoryV4Request;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.common.type.Versioned;

public class AmbariRepositoryV4Validator implements ConstraintValidator<ValidAmbariRepo, AmbariRepositoryV4Request> {

    public static final String MIN_AMBARI_VERSION = "2.7";

    @Override
    public void initialize(ValidAmbariRepo constraintAnnotation) {
    }

    @Override
    public boolean isValid(AmbariRepositoryV4Request ambariRepoDetailsJson, ConstraintValidatorContext context) {
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
