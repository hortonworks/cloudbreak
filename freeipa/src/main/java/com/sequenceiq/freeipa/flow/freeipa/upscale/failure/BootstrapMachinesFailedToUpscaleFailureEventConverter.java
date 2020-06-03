package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesFailed;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class BootstrapMachinesFailedToUpscaleFailureEventConverter implements PayloadConverter<UpscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return BootstrapMachinesFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleFailureEvent convert(Object payload) {
        BootstrapMachinesFailed result = (BootstrapMachinesFailed) payload;
        UpscaleFailureEvent event = new UpscaleFailureEvent(result.getResourceId(), "Bootstrapping mahcines", Set.of(), Map.of(),
                new Exception("Payload failed: " + payload));
        return event;
    }
}
