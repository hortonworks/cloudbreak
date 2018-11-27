package com.sequenceiq.cloudbreak.validation;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosRequest;

public class KerberosRequestValidator implements ConstraintValidator<ValidKerberosRequest, KerberosRequest> {

    @Override
    public boolean isValid(KerberosRequest req, ConstraintValidatorContext constraintValidatorContext) {
        return isValid(req);
    }

    public static boolean isValid(KerberosRequest request) {
        return List.of(request.getActiveDirectory() != null, request.getFreeIpa() != null,
                    request.getMit() != null, request.getAmbariKerberosDescriptor() != null)
                .stream()
                .filter(fieldIsNotNull -> fieldIsNotNull)
                .count() == 1 && StringUtils.isNotEmpty(request.getName());
    }

}
