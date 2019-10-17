package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.host.RemoveHostsResponse;

public class RemoveHostsResponseToCleanupFailureEventConverter implements PayloadConverter<CleanupFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return RemoveHostsResponse.class.isAssignableFrom(sourceClass);
    }

    @Override
    public CleanupFailureEvent convert(Object payload) {
        RemoveHostsResponse removeHostsResponse = (RemoveHostsResponse) payload;
        CleanupFailureEvent event = new CleanupFailureEvent(removeHostsResponse, "DNS record removal", removeHostsResponse.getHostCleanupFailed(),
                removeHostsResponse.getHostCleanupSuccess());
        return event;
    }
}
