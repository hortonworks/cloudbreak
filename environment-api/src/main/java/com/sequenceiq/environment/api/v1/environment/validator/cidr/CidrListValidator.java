package com.sequenceiq.environment.api.v1.environment.validator.cidr;

import static com.sequenceiq.cloudbreak.validation.CidrValidatorHelper.isCidrPatternMatched;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.google.common.base.CharMatcher;

public class CidrListValidator implements ConstraintValidator<ValidCidrList, String> {
    @Override
    public void initialize(ValidCidrList constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        boolean result = true;
        if (value != null) {
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
