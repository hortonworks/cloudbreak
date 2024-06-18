package com.sequenceiq.freeipa.api.v1.dns.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsPtrRecordRequest;

public class AddDnsPtrRecordRequestValidator  implements ConstraintValidator<ValidAddDnsPtrRecordRequest, AddDnsPtrRecordRequest> {
    @Override
    public boolean isValid(AddDnsPtrRecordRequest addDnsPtrRecordRequest, ConstraintValidatorContext constraintValidatorContext) {
        return PtrRecordValidationUtil.isIpInZoneRange(addDnsPtrRecordRequest.getIp(), addDnsPtrRecordRequest.getReverseDnsZone(), constraintValidatorContext);
    }
}
