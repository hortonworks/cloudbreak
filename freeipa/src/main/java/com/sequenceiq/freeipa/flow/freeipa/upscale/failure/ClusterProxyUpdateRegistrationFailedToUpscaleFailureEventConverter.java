package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationFailed;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverter implements PayloadConverter<UpscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ClusterProxyUpdateRegistrationFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleFailureEvent convert(Object payload) {
        ClusterProxyUpdateRegistrationFailed result = (ClusterProxyUpdateRegistrationFailed) payload;
        return new UpscaleFailureEvent(result.getResourceId(), "Updating cluster proxy", Set.of(), ERROR, Map.of(),
                result.getException());
    }
}