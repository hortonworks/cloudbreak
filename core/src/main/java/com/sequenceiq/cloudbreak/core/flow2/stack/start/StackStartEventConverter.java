package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import com.sequenceiq.cloudbreak.core.flow2.EventConverterBase;

public class StackStartEventConverter extends EventConverterBase<StackStartEvent> {
    public StackStartEventConverter() {
        super(StackStartEvent.class);
    }
}
