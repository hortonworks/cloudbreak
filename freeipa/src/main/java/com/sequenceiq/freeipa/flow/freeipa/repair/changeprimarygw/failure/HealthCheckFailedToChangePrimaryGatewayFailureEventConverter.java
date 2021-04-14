package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.failure;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayFailureEvent;
import com.sequenceiq.freeipa.flow.stack.HealthCheckFailed;

public class HealthCheckFailedToChangePrimaryGatewayFailureEventConverter implements PayloadConverter<ChangePrimaryGatewayFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return HealthCheckFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public ChangePrimaryGatewayFailureEvent convert(Object payload) {
        HealthCheckFailed result = (HealthCheckFailed) payload;
        return new ChangePrimaryGatewayFailureEvent(result.getResourceId(), "Checking health failed", Set.of(), Map.of(), result.getException());
    }
}
