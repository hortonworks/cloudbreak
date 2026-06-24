package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade;

import com.sequenceiq.cloudbreak.cloud.event.resource.validation.SecurityGroupValidationResult;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class SecurityGroupValidationResultToStackEventConverter implements PayloadConverter<StackEvent> {

    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return SecurityGroupValidationResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public StackEvent convert(Object payload) {
        return new StackEvent(((SecurityGroupValidationResult) payload).getResourceId());
    }
}
