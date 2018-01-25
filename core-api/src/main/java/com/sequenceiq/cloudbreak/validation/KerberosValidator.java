package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.type.KerberosType;

public class KerberosValidator implements ConstraintValidator<ValidKerberos, KerberosRequest> {

    @Override
    public void initialize(ValidKerberos constraintAnnotation) {
    }

    @Override
    public boolean isValid(KerberosRequest request, ConstraintValidatorContext context) {
        return KerberosType.valueOf(request) != null;
    }
}