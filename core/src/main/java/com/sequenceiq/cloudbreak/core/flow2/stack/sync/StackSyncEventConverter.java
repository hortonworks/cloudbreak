package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import com.sequenceiq.cloudbreak.core.flow2.EventConverter;

public class StackSyncEventConverter implements EventConverter<StackSyncEvent> {
    @Override
    public StackSyncEvent convert(String key) {
        return StackSyncEvent.fromString(key);
    }
}
