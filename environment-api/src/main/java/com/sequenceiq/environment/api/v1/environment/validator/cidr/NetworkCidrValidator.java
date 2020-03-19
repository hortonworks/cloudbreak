package com.sequenceiq.environment.api.v1.environment.validator.cidr;

import static com.sequenceiq.cloudbreak.validation.CidrValidatorHelper.isCidrPatternMatched;
import static com.sequenceiq.cloudbreak.validation.CidrValidatorHelper.isPrefixLengthCouldBeDividedIntoSubnets;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

public class NetworkCidrValidator implements ConstraintValidator<NetworkCidr, String> {
    @Override
    public void initialize(NetworkCidr constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isEmpty(value)) {
            return true;
        }
        return isCidrPatternMatched(value) && isPrefixLengthCouldBeDividedIntoSubnets(value);
    }
}
