package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade;

import com.sequenceiq.cloudbreak.cloud.event.resource.validation.SecurityGroupValidationResult;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureEvent;

public class SecurityGroupValidationResultToPrepareUpgradeFailureConverter implements PayloadConverter<PrepareUpgradeFailureEvent> {

    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return SecurityGroupValidationResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public PrepareUpgradeFailureEvent convert(Object payload) {
        SecurityGroupValidationResult result = (SecurityGroupValidationResult) payload;
        return new PrepareUpgradeFailureEvent(result.getResourceId(), FailureType.VALIDATION, result.getErrorDetails());
    }
}
