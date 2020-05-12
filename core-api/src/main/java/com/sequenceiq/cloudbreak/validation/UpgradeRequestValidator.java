package com.sequenceiq.cloudbreak.validation;

import java.util.Objects;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;

public class UpgradeRequestValidator implements ConstraintValidator<ValidUpgradeRequest, UpgradeV4Request> {

    @Override
    public boolean isValid(UpgradeV4Request value, ConstraintValidatorContext context) {
        if (Objects.isNull(value)) {
            return false;
        }
        return isOsUpgrade(value) || isRuntimeUpgrade(value);
    }

    private boolean isOsUpgrade(UpgradeV4Request request) {
        return Boolean.TRUE.equals(request.getLockComponents()) && StringUtils.isEmpty(request.getRuntime());
    }

    private boolean isRuntimeUpgrade(UpgradeV4Request request) {
        return !Boolean.TRUE.equals(request.getLockComponents())
                && ((!StringUtils.isEmpty(request.getRuntime()) && StringUtils.isEmpty(request.getImageId()))
                || (!StringUtils.isEmpty(request.getImageId()) && StringUtils.isEmpty(request.getRuntime())));
    }
}