package com.sequenceiq.freeipa.flow.freeipa.downscale.failure;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert.RevokeCertsResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;

public class RevokeCertsResponseToDownscaleFailureEventConverter implements PayloadConverter<DownscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return RevokeCertsResponse.class.isAssignableFrom(sourceClass);
    }

    @Override
    public DownscaleFailureEvent convert(Object payload) {
        RevokeCertsResponse revokeCertsResponse = (RevokeCertsResponse) payload;
        DownscaleFailureEvent event = new DownscaleFailureEvent(revokeCertsResponse.getResourceId(), "Cert revocation",
                revokeCertsResponse.getCertCleanupSuccess(), revokeCertsResponse.getCertCleanupFailed(), new Exception("Payload failed: " + payload));
        return event;
    }
}
