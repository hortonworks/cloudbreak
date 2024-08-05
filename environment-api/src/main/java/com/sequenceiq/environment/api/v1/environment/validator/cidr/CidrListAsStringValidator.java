package com.sequenceiq.environment.api.v1.environment.validator.cidr;

import static com.sequenceiq.cloudbreak.validation.CidrValidatorHelper.isCidrPatternMatched;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.CharMatcher;

public class CidrListAsStringValidator implements ConstraintValidator<ValidCidrListAsString, String> {

    @Override
    public void initialize(ValidCidrListAsString constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        boolean result = true;
        if (StringUtils.isNotEmpty(value)) {
            String[] cidrs = value.split(",");
            int numberOfCommas = CharMatcher.is(',').countIn(value);
            int count = 0;
            for (String cidr : cidrs) {
                if (!isCidrPatternMatched(cidr)) {
                    result = false;
                }
                count++;
            }
            // more commas then expected
            if (count != numberOfCommas + 1) {
                result = false;
            }
        } else {
            result = true;
        }
        return result;
    }
}
