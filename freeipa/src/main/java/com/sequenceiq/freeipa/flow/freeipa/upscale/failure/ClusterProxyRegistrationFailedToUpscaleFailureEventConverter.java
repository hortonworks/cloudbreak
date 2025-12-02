package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationFailed;

public class ClusterProxyRegistrationFailedToUpscaleFailureEventConverter implements PayloadConverter<UpscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ClusterProxyRegistrationFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleFailureEvent convert(Object payload) {
        ClusterProxyRegistrationFailed clusterProxyRegistrationFailed = (ClusterProxyRegistrationFailed) payload;
        return new UpscaleFailureEvent(
                clusterProxyRegistrationFailed.getResourceId(),
                "ClusterProxyRegistration",
                Set.of(),
                ERROR,
                Map.of(),
                clusterProxyRegistrationFailed.getException()
        );
    }

}