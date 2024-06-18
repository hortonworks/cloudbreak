package com.sequenceiq.freeipa.api.v1.dns.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.sequenceiq.freeipa.api.v1.dns.model.DeleteDnsPtrRecordRequest;

public class DeleteDnsPtrRecordRequestValidator implements ConstraintValidator<ValidDeleteDnsPtrRecordRequest, DeleteDnsPtrRecordRequest> {
    @Override
    public boolean isValid(DeleteDnsPtrRecordRequest deleteDnsPtrRecordRequest, ConstraintValidatorContext constraintValidatorContext) {
        return PtrRecordValidationUtil.isIpInZoneRange(deleteDnsPtrRecordRequest.getIp(), deleteDnsPtrRecordRequest.getReverseDnsZone(),
                constraintValidatorContext);
    }
}
