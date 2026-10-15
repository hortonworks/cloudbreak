package com.sequenceiq.sdx.validation;

import java.util.Objects;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.common.api.util.ValidatorUtil;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;

public class UpgradeRequestValidator implements ConstraintValidator<ValidUpgradeRequest, SdxUpgradeRequest> {

    @Override
    public boolean isValid(SdxUpgradeRequest request, ConstraintValidatorContext context) {
        if (!validateEmptyRequest(request, context)) {
            return false;
        }
        if (!mutuallyExclusiveDryRunOrShowImages(request, context)) {
            return false;
        }
        return isOsUpgrade(request) || isRuntimeUpgrade(request) || request.isDryRunOnly() || request.isShowAvailableImagesOnly() || request.isEmpty();
    }

    private boolean validateEmptyRequest(SdxUpgradeRequest request, ConstraintValidatorContext context) {
        if (Objects.isNull(request)) {
            String msg = "Invalid upgrade request: empty content";
            ValidatorUtil.addConstraintViolation(context, msg).disableDefaultConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean mutuallyExclusiveDryRunOrShowImages(SdxUpgradeRequest request, ConstraintValidatorContext context) {
        if (request.isDryRun()  && request.isShowAvailableImagesSet()) {
            String msg = "Invalid upgrade request: 'dry-run' cannot be used in parallel with  'show-available-images' or "
                    + "'show-latest-available-image-per-runtime' in the request";
            ValidatorUtil.addConstraintViolation(context, msg).disableDefaultConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean isOsUpgrade(SdxUpgradeRequest request) {
        return Boolean.TRUE.equals(request.getLockComponents()) && StringUtils.isEmpty(request.getRuntime());
    }

    private boolean isRuntimeUpgrade(SdxUpgradeRequest request) {
        return !Boolean.TRUE.equals(request.getLockComponents())
                && (!StringUtils.isEmpty(request.getRuntime()) && StringUtils.isEmpty(request.getImageId())
                || !StringUtils.isEmpty(request.getImageId()) && StringUtils.isEmpty(request.getRuntime()));
    }
}

