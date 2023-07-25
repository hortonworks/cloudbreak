package com.sequenceiq.cloudbreak.rotation.validator;

import java.util.Collections;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidSecretTypes;
import com.sequenceiq.common.api.util.ValidatorUtil;

public class SecretTypesValidator implements ConstraintValidator<ValidSecretTypes, List<String>> {

    private Class<? extends SecretType>[] allowedTypes;

    private boolean internalOnlyAllowed;

    private boolean multiAllowed;

    @Override
    public void initialize(ValidSecretTypes constraintAnnotation) {
        allowedTypes = constraintAnnotation.allowedTypes();
        internalOnlyAllowed = constraintAnnotation.internalOnlyAllowed();
        multiAllowed = constraintAnnotation.multiAllowed();
    }

    @Override
    public boolean isValid(List<String> secrets, ConstraintValidatorContext context) {
        try {
            if (secrets.stream().anyMatch(secret -> Collections.frequency(secrets, secret) > 1)) {
                ValidatorUtil.addConstraintViolation(context, "There is at least one duplication in the request!");
                return false;
            }
            List<SecretType> secretTypes = SecretTypeConverter.mapSecretTypes(secrets, Sets.newHashSet(allowedTypes));
            if (internalOnlyAllowed && !secretTypes.stream().allMatch(SecretType::internal)) {
                ValidatorUtil.addConstraintViolation(context, "Only internal secret type is allowed!");
                return false;
            }
            if (!multiAllowed && secretTypes.stream().anyMatch(SecretType::multiSecret)) {
                ValidatorUtil.addConstraintViolation(context, "Only single secret type is allowed!");
                return false;
            }
        } catch (Exception e) {
            ValidatorUtil.addConstraintViolation(context, e.getMessage());
            return false;
        }
        return true;
    }
}
