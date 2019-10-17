package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.users.RemoveUsersResponse;

public class RemoveUsersResponseToCleanupFailureEventConverter implements PayloadConverter<CleanupFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return RemoveUsersResponse.class.isAssignableFrom(sourceClass);
    }

    @Override
    public CleanupFailureEvent convert(Object payload) {
        RemoveUsersResponse removeUsersResponse = (RemoveUsersResponse) payload;
        CleanupFailureEvent event = new CleanupFailureEvent(removeUsersResponse,
                "DNS record removal", removeUsersResponse.getUserCleanupFailed(), removeUsersResponse.getUserCleanupSuccess());
        return event;
    }
}
