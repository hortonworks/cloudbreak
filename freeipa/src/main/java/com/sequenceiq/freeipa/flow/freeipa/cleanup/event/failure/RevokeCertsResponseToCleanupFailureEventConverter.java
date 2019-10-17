package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert.RevokeCertsResponse;

public class RevokeCertsResponseToCleanupFailureEventConverter implements PayloadConverter<CleanupFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return RevokeCertsResponse.class.isAssignableFrom(sourceClass);
    }

    @Override
    public CleanupFailureEvent convert(Object payload) {
        RevokeCertsResponse revokeCertsResponse = (RevokeCertsResponse) payload;
        CleanupFailureEvent event = new CleanupFailureEvent(revokeCertsResponse,
                "Cert revocation", revokeCertsResponse.getCertCleanupFailed(), revokeCertsResponse.getCertCleanupSuccess());
        return event;
    }
}
