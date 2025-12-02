package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerUpdateFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class LoadBalancerUpdateFailureEventToUpscaleFailureEventConverter implements PayloadConverter<UpscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return LoadBalancerUpdateFailureEvent.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleFailureEvent convert(Object payload) {
        LoadBalancerUpdateFailureEvent result = (LoadBalancerUpdateFailureEvent) payload;
        return new UpscaleFailureEvent(
                result.getResourceId(),
                "Load balancer update",
                Set.of(),
                ERROR,
                result.getException() != null ? Map.of("statusReason", result.getException().getMessage()) : Map.of(),
                result.getException()
        );
    }
}
