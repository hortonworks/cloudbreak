package com.sequenceiq.sdx.validation;

import static com.sequenceiq.cloudbreak.validation.UpgradeRequestValidator.COMPONENTS_OPTIONS_ERROR;
import static com.sequenceiq.cloudbreak.validation.UpgradeRequestValidator.getComponentsContentError;

import java.util.Objects;
import java.util.Optional;

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
        if (request.getComponents() != null) {
            return validateComponents(request, context);
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

    private boolean validateComponents(SdxUpgradeRequest request, ConstraintValidatorContext context) {
        Optional<String> error;
        if (Boolean.TRUE.equals(request.getLockComponents()) || request.isDryRun() || request.isShowAvailableImagesSet()
                || StringUtils.isNotBlank(request.getImageId()) || StringUtils.isNotBlank(request.getRuntime())) {
            error = Optional.of(COMPONENTS_OPTIONS_ERROR);
        } else {
            error = getComponentsContentError(request.getComponents());
        }
        error.ifPresent(message -> ValidatorUtil.addConstraintViolation(context, message).disableDefaultConstraintViolation());
        return error.isEmpty();
    }

}

