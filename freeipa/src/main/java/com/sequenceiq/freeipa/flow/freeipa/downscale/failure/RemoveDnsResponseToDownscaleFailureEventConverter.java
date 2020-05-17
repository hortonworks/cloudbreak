package com.sequenceiq.freeipa.flow.freeipa.downscale.failure;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns.RemoveDnsResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;

public class RemoveDnsResponseToDownscaleFailureEventConverter implements PayloadConverter<DownscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return RemoveDnsResponse.class.isAssignableFrom(sourceClass);
    }

    @Override
    public DownscaleFailureEvent convert(Object payload) {
        RemoveDnsResponse removeDnsResponse = (RemoveDnsResponse) payload;
        DownscaleFailureEvent event = new DownscaleFailureEvent(removeDnsResponse.getResourceId(), "DNS record removal",
                removeDnsResponse.getDnsCleanupSuccess(), removeDnsResponse.getDnsCleanupFailed(), new Exception("Payload failed: " + payload));
        return event;
    }
}
