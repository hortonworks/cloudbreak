package com.sequenceiq.cloudbreak.rotation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidSecretType;
import com.sequenceiq.common.api.util.ValidatorUtil;

public class SecretTypeValidator implements ConstraintValidator<ValidSecretType, String> {

    private Class<? extends SecretType>[] allowedTypes;

    @Override
    public void initialize(ValidSecretType constraintAnnotation) {
        allowedTypes = constraintAnnotation.allowedTypes();
    }

    @Override
    public boolean isValid(String secret, ConstraintValidatorContext context) {
        try {
            SecretType secretType = SecretTypeConverter.mapSecretType(secret, Sets.newHashSet(allowedTypes));
            if (!RegionAwareInternalCrnGeneratorUtil.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn()) && secretType.internal()) {
                ValidatorUtil.addConstraintViolation(context, "Internal secret types can be rotated only by using internal actor!");
                return false;
            }
        } catch (Exception e) {
            ValidatorUtil.addConstraintViolation(context, e.getMessage());
            return false;
        }
        return true;
    }
}
