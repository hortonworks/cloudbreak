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

    private boolean internalAllowed;

    @Override
    public void initialize(ValidSecretTypes constraintAnnotation) {
        allowedTypes = constraintAnnotation.allowedTypes();
        internalAllowed = constraintAnnotation.internalAllowed();
    }

    @Override
    public boolean isValid(List<String> secrets, ConstraintValidatorContext context) {
        try {
            if (secrets.stream().anyMatch(secret -> Collections.frequency(secrets, secret) > 1)) {
                ValidatorUtil.addConstraintViolation(context, "There is at least one duplication in the request!");
                return false;
            }
            List<SecretType> secretTypes = SecretTypeConverter.mapSecretTypes(secrets, Sets.newHashSet(allowedTypes));
            if (!internalAllowed && secretTypes.stream().anyMatch(SecretType::internal)) {
                return false;
            }
            if (secretTypes.stream().filter(SecretType::multiSecret).count() > 1) {
                ValidatorUtil.addConstraintViolation(context, "Request should contain maximum 1 secret type which affects multiple resources!");
                return false;
            }
        } catch (Exception e) {
            ValidatorUtil.addConstraintViolation(context, e.getMessage());
            return false;
        }
        return true;
    }
}
