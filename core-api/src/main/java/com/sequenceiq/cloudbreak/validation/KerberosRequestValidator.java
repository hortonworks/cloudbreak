package com.sequenceiq.cloudbreak.validation;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;

public class KerberosRequestValidator implements ConstraintValidator<ValidKerberosRequest, KerberosV4Request> {

    @Override
    public boolean isValid(KerberosV4Request req, ConstraintValidatorContext constraintValidatorContext) {
        return isValid(req);
    }

    public static boolean isValid(KerberosV4Request request) {
        return List.of(request.getActiveDirectory() != null, request.getFreeIpa() != null,
                    request.getMit() != null, request.getAmbariDescriptor() != null)
                .stream()
                .filter(fieldIsNotNull -> fieldIsNotNull)
                .count() == 1 && StringUtils.isNotEmpty(request.getName());
    }

}
