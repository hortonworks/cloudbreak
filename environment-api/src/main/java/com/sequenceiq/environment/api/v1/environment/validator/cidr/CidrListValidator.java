package com.sequenceiq.environment.api.v1.environment.validator.cidr;

import static com.sequenceiq.cloudbreak.validation.CidrValidatorHelper.isCidrPatternMatched;

import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.collections4.CollectionUtils;

public class CidrListValidator implements ConstraintValidator<ValidCidrList, List<String>> {

    @Override
    public void initialize(ValidCidrList constraintAnnotation) {
    }

    @Override
    public boolean isValid(List<String> cidrs, ConstraintValidatorContext context) {
        if (CollectionUtils.isEmpty(cidrs)) {
            return true;
        } else {
            return cidrs.stream().allMatch(cidr -> isCidrPatternMatched(cidr));
        }
    }
}
