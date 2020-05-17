package com.sequenceiq.freeipa.flow.freeipa.downscale.failure;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationFailed;

public class ClusterProxyUpdateRegistrationFailedToDownscaleFailureEventConverter implements PayloadConverter<DownscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ClusterProxyUpdateRegistrationFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public DownscaleFailureEvent convert(Object payload) {
        ClusterProxyUpdateRegistrationFailed registrationFailed = (ClusterProxyUpdateRegistrationFailed) payload;
        DownscaleFailureEvent event = new DownscaleFailureEvent(registrationFailed.getResourceId(), "Downscale Cluster Proxy Registration",
                Set.of(), Map.of(), new Exception("Payload failed: " + payload));
        return event;
    }
}
