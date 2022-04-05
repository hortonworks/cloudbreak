package com.sequenceiq.cloudbreak.auth.security.internal;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;

public class AccountIdValidator implements ConstraintValidator<AccountId, String> {

    @Override
    public boolean isValid(String req, ConstraintValidatorContext constraintValidatorContext) {
        constraintValidatorContext.disableDefaultConstraintViolation();

        if (!RegionAwareInternalCrnGeneratorUtil.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn())) {
            return true;
        } else if (StringUtils.isBlank(req)) {
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("In case of internal actor API call you need to specify account id.")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

}
