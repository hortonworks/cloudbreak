package com.sequenceiq.sdx.validation;

import java.util.Objects;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;

public class UpgradeRequestValidator implements ConstraintValidator<ValidUpgradeRequest, SdxUpgradeRequest> {

    @Override
    public boolean isValid(SdxUpgradeRequest value, ConstraintValidatorContext context) {
        if (!validateEmptyRequest(value, context)) {
            return false;
        }
        if (!mutuallyExclusiveDryRunOrShowImages(value, context)) {
            return false;
        }
        return isOsUpgrade(value) || isRuntimeUpgrade(value) || value.isDryRunOnly() || value.isShowAvailableImagesOnly() || value.isEmpty();
    }

    private boolean validateEmptyRequest(SdxUpgradeRequest value, ConstraintValidatorContext context) {
        if (Objects.isNull(value)) {
            String msg = "Invalid upgrade request: empty content";
            context.buildConstraintViolationWithTemplate(msg).addConstraintViolation().disableDefaultConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean mutuallyExclusiveDryRunOrShowImages(SdxUpgradeRequest request, ConstraintValidatorContext context) {
        if (request.isDryRun()  && request.isShowAvailableImagesSet()) {
            String msg = "Invalid upgrade request: 'dry-run' cannot be used in parallel with  'show-available-images' or "
                    + "'show-latest-available-image-per-runtime' in the request";
            context.buildConstraintViolationWithTemplate(msg).addConstraintViolation().disableDefaultConstraintViolation();
            return false;
        }
        return true;
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