package com.sequenceiq.cloudbreak.validation;

import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.util.StringUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackRequest;

public class ManagementPackResourceValidator implements ConstraintValidator<ValidManagementPack, ManagementPackRequest> {
    private Set<String> validPurgeListElements;

    @Override
    public void initialize(ValidManagementPack constraintAnnotation) {
        validPurgeListElements = Sets.newHashSet();
        validPurgeListElements.add("stack-definitions");
        validPurgeListElements.add("service-definitions");
        validPurgeListElements.add("mpacks");
    }

    @Override
    public boolean isValid(ManagementPackRequest value, ConstraintValidatorContext context) {
        boolean result = true;
        if (StringUtils.isEmpty(value.getMpackUrl())) {
            addConstraintViolation(context, "mpackUrl", "mpackUrl cannot be empty");
            result = false;
        }
        if (!value.isPurge() && !value.getPurgeList().isEmpty()) {
            addConstraintViolation(context, "purgeList", "purgeList have to be empty if purge option is false");
            result = false;
        }
        if (!isValidPurgeList(value, context)) {
            result = false;
        }
        return result;
    }

    private boolean isValidPurgeList(ManagementPackRequest value, ConstraintValidatorContext context) {
        boolean result = true;
        if (value.getPurgeList().stream().anyMatch(p -> !validPurgeListElements.contains(p))) {
            addConstraintViolation(context, "purgeList", String.format("purgelist contains only elements from %s", validPurgeListElements.stream().collect(
                    Collectors.joining(","))));
            result = false;
        }
        return result;
    }

    private void addConstraintViolation(ConstraintValidatorContext context, String property, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(property)
                .addConstraintViolation();
    }
}
