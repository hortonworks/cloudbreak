package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import com.sequenceiq.cloudbreak.core.flow2.EventConverter;

public class StackTerminationEventConverter implements EventConverter<StackTerminationEvent> {
    @Override
    public StackTerminationEvent convert(String key) {
        return StackTerminationEvent.fromString(key);
    }
}
