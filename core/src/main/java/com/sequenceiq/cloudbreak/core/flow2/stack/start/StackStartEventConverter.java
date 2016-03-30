package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import com.sequenceiq.cloudbreak.core.flow2.EventConverterAdapter;

public class StackStartEventConverter extends EventConverterAdapter<StackStartEvent> {
    public StackStartEventConverter() {
        super(StackStartEvent.class);
    }
}
