package com.sequenceiq.cloudbreak.validation;

import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.util.StringUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.request.ManagementPackV4Request;

public class ManagementPackResourceValidator implements ConstraintValidator<ValidManagementPack, ManagementPackV4Request> {
    private Set<String> validPurgeListElements;

    @Override
    public void initialize(ValidManagementPack constraintAnnotation) {
        validPurgeListElements = Sets.newHashSet();
        validPurgeListElements.add("stack-definitions");
        validPurgeListElements.add("service-definitions");
        validPurgeListElements.add("mpacks");
    }

    @Override
    public boolean isValid(ManagementPackV4Request value, ConstraintValidatorContext context) {
        boolean result = true;
        if (StringUtils.isEmpty(value.getMpackUrl())) {
            ValidatorUtil.addConstraintViolation(context, "mpackUrl cannot be empty", "mpackUrl");
            result = false;
        }
        if (!value.isPurge() && !value.getPurgeList().isEmpty()) {
            ValidatorUtil.addConstraintViolation(context, "purgeList have to be empty if purge option is false", "purgeList");
            result = false;
        }
        if (!isValidPurgeList(value, context)) {
            result = false;
        }
        return result;
    }

    private boolean isValidPurgeList(ManagementPackV4Request value, ConstraintValidatorContext context) {
        boolean result = true;
        if (value.getPurgeList().stream().anyMatch(p -> !validPurgeListElements.contains(p))) {
            ValidatorUtil.addConstraintViolation(context, String.format("purgelist contains only elements from %s", String.join(",", validPurgeListElements)),
                    "purgeList");
            result = false;
        }
        return result;
    }

}
