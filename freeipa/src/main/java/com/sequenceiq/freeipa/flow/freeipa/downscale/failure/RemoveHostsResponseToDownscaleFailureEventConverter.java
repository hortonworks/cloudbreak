package com.sequenceiq.freeipa.flow.freeipa.downscale.failure;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.host.RemoveHostsResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;

public class RemoveHostsResponseToDownscaleFailureEventConverter implements PayloadConverter<DownscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return RemoveHostsResponse.class.isAssignableFrom(sourceClass);
    }

    @Override
    public DownscaleFailureEvent convert(Object payload) {
        RemoveHostsResponse removeHostsResponse = (RemoveHostsResponse) payload;
        DownscaleFailureEvent event = new DownscaleFailureEvent(removeHostsResponse.getResourceId(),  "Hosts removal",
                removeHostsResponse.getHostCleanupSuccess(), removeHostsResponse.getHostCleanupFailed(), new Exception("Payload failed: " + payload));
        return event;
    }
}
