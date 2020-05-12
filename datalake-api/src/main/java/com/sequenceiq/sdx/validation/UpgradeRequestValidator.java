package com.sequenceiq.sdx.validation;

import java.util.Objects;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;

public class UpgradeRequestValidator implements ConstraintValidator<ValidUpgradeRequest, SdxUpgradeRequest> {

    @Override
    public boolean isValid(SdxUpgradeRequest value, ConstraintValidatorContext context) {
        if (Objects.isNull(value)) {
            return false;
        }
        return isOsUpgrade(value) || isRuntimeUpgrade(value);
    }

    private boolean isOsUpgrade(SdxUpgradeRequest request) {
        return Boolean.TRUE.equals(request.getLockComponents()) && StringUtils.isEmpty(request.getRuntime());
    }

    private boolean isRuntimeUpgrade(SdxUpgradeRequest request) {
        return !Boolean.TRUE.equals(request.getLockComponents())
                && ((!StringUtils.isEmpty(request.getRuntime()) && StringUtils.isEmpty(request.getImageId()))
                || (!StringUtils.isEmpty(request.getImageId()) && StringUtils.isEmpty(request.getRuntime())));
    }
}