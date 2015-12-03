package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import com.sequenceiq.cloudbreak.core.flow2.EventConverter;

public class StackCreationEventConverter implements EventConverter<StackCreationEvent> {
    @Override
    public StackCreationEvent convert(String key) {
        return StackCreationEvent.fromString(key);
    }
}
