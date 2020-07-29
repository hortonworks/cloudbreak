package com.sequenceiq.cloudbreak.validation;

import java.util.Objects;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;

public class UpgradeRequestValidator implements ConstraintValidator<ValidUpgradeRequest, UpgradeV4Request> {

    @Override
    public boolean isValid(UpgradeV4Request value, ConstraintValidatorContext context) {
        if (!validateEmptyRequest(value, context)) {
            return false;
        }
        if (!mutuallyExclusiveDryRunOrShowImages(value, context)) {
            return false;
        }
        return isOsUpgrade(value) || isRuntimeUpgrade(value) || value.isDryRunOnly() || value.isShowAvailableImagesOnly() || value.isEmpty();
    }

    private boolean validateEmptyRequest(UpgradeV4Request value, ConstraintValidatorContext context) {
        if (Objects.isNull(value)) {
            String msg = "Invalid upgrade request: empty content";
            context.buildConstraintViolationWithTemplate(msg).addConstraintViolation().disableDefaultConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean mutuallyExclusiveDryRunOrShowImages(UpgradeV4Request request, ConstraintValidatorContext context) {
        if (request.isDryRun()  && request.isShowAvailableImagesSet()) {
            String msg = "Invalid upgrade request: 'dry-run' cannot be used in parallel with  'show-available-images' or "
                    + "'show-latest-available-image-per-runtime' in the request";
            context.buildConstraintViolationWithTemplate(msg).addConstraintViolation().disableDefaultConstraintViolation();
            return false;
        }
        return true;
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