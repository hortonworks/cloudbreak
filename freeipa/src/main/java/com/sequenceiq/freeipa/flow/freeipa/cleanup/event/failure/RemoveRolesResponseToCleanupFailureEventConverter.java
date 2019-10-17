package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.roles.RemoveRolesResponse;

public class RemoveRolesResponseToCleanupFailureEventConverter implements PayloadConverter<CleanupFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return RemoveRolesResponse.class.isAssignableFrom(sourceClass);
    }

    @Override
    public CleanupFailureEvent convert(Object payload) {
        RemoveRolesResponse removeRolesResponse = (RemoveRolesResponse) payload;
        CleanupFailureEvent event = new CleanupFailureEvent(removeRolesResponse,
                "DNS record removal", removeRolesResponse.getRoleCleanupFailed(), removeRolesResponse.getRoleCleanupSuccess());
        return event;
    }
}
