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
        if (StringUtils.isEmpty(value.getMpackUrl())) {
            ValidatorUtil.addConstraintViolation(context, "mpackUrl cannot be empty", "mpackUrl");
            return false;
        }
        if (!value.isPurge() && !value.getPurgeList().isEmpty()) {
            ValidatorUtil.addConstraintViolation(context, "purgeList have to be empty if purge option is false", "purgeList");
            return false;
        }
        return isValidPurgeList(value, context);
    }

    private boolean isValidPurgeList(ManagementPackV4Request value, ConstraintValidatorContext context) {
        if (value.getPurgeList().stream().anyMatch(p -> !validPurgeListElements.contains(p))) {
            ValidatorUtil.addConstraintViolation(context, String.format("purgelist contains only elements from %s",
                    String.join(",", validPurgeListElements)), "purgeList");
            return false;
        }
        return true;
    }

}
