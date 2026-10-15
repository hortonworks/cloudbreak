package com.sequenceiq.distrox.api.v1.distrox.model.upgrade;

import java.util.Objects;
import java.util.Optional;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.validation.UpgradeRequestValidator;
import com.sequenceiq.common.api.util.ValidatorUtil;

public class DistroXUpgradeRequestValidator implements ConstraintValidator<ValidUpgradeRequest, DistroXUpgradeV1Request> {

    @Override
    public boolean isValid(DistroXUpgradeV1Request request, ConstraintValidatorContext context) {
        if (!validateEmptyRequest(request, context)) {
            return false;
        }
        if (!mutuallyExclusiveDryRunOrShowImages(request, context)) {
            return false;
        }
        if (request.getComponents() != null) {
            return validateComponents(request, context);
        }
        return isOsUpgrade(request) || isRuntimeUpgrade(request) || request.isDryRunOnly() || request.isShowAvailableImagesOnly() || request.isEmpty();
    }

    private boolean validateComponents(DistroXUpgradeV1Request request, ConstraintValidatorContext context) {
        Optional<String> error = getComponentsOptionsError(request)
                .or(() -> UpgradeRequestValidator.getComponentsContentError(request.getComponents()));
        error.ifPresent(message -> ValidatorUtil.addConstraintViolation(context, message).disableDefaultConstraintViolation());
        return error.isEmpty();
    }

    private static Optional<String> getComponentsOptionsError(DistroXUpgradeV1Request request) {
        if (Boolean.TRUE.equals(request.getLockComponents()) || request.isDryRun() || request.isShowAvailableImagesSet()
                || StringUtils.isNotBlank(request.getImageId()) || StringUtils.isNotBlank(request.getRuntime())) {
            return Optional.of(UpgradeRequestValidator.COMPONENTS_OPTIONS_ERROR);
        }
        return Optional.empty();
    }

    private boolean validateEmptyRequest(DistroXUpgradeV1Request request, ConstraintValidatorContext context) {
        if (Objects.isNull(request)) {
            String msg = "Invalid upgrade request: empty content";
            ValidatorUtil.addConstraintViolation(context, msg).disableDefaultConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean mutuallyExclusiveDryRunOrShowImages(DistroXUpgradeV1Request request, ConstraintValidatorContext context) {
        if (request.isDryRun()  && request.isShowAvailableImagesSet()) {
            String msg = "Invalid upgrade request: 'dry-run' cannot be used in parallel with  'show-available-images' or "
                    + "'show-latest-available-image-per-runtime' in the request";
            ValidatorUtil.addConstraintViolation(context, msg).disableDefaultConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean isOsUpgrade(DistroXUpgradeV1Request request) {
        return Boolean.TRUE.equals(request.getLockComponents()) && StringUtils.isEmpty(request.getRuntime());
    }

    private boolean isRuntimeUpgrade(DistroXUpgradeV1Request request) {
        return !Boolean.TRUE.equals(request.getLockComponents())
                && (!StringUtils.isEmpty(request.getRuntime()) && StringUtils.isEmpty(request.getImageId())
                || !StringUtils.isEmpty(request.getImageId()) && StringUtils.isEmpty(request.getRuntime()));
    }
}
