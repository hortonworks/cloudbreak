package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import com.sequenceiq.cloudbreak.core.flow2.EventConverter;

public class InstanceTerminationEventConverter implements EventConverter<InstanceTerminationEvent> {
    @Override
    public InstanceTerminationEvent convert(String key) {
        return InstanceTerminationEvent.fromString(key);
    }
}
