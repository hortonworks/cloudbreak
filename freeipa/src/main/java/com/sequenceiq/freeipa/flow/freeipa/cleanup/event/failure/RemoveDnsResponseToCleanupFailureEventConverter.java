package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns.RemoveDnsResponse;

public class RemoveDnsResponseToCleanupFailureEventConverter implements PayloadConverter<CleanupFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return RemoveDnsResponse.class.isAssignableFrom(sourceClass);
    }

    @Override
    public CleanupFailureEvent convert(Object payload) {
        RemoveDnsResponse removeDnsResponse = (RemoveDnsResponse) payload;
        CleanupFailureEvent event = new CleanupFailureEvent(removeDnsResponse, "DNS record removal", removeDnsResponse.getDnsCleanupFailed(),
                removeDnsResponse.getDnsCleanupSuccess());
        return event;
    }
}
