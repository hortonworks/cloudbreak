package com.sequenceiq.freeipa.api.v1.kerberos.validation;

import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;

public class KerberosRequestValidator implements ConstraintValidator<ValidKerberosRequest, CreateKerberosConfigRequest> {
    @Override
    public boolean isValid(CreateKerberosConfigRequest req, ConstraintValidatorContext constraintValidatorContext) {
        return isValid(req);
    }

    public static boolean isValid(CreateKerberosConfigRequest request) {
        return List.of(request.getActiveDirectory() != null, request.getFreeIpa() != null,
                    request.getMit() != null)
                .stream()
                .filter(fieldIsNotNull -> fieldIsNotNull)
                .count() == 1 && StringUtils.isNotEmpty(request.getName());
    }
}
