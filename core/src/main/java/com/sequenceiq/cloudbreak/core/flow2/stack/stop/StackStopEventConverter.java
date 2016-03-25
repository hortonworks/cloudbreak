package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import com.sequenceiq.cloudbreak.core.flow2.EventConverterBase;

public class StackStopEventConverter extends EventConverterBase<StackStopEvent> {
    public StackStopEventConverter() {
        super(StackStopEvent.class);
    }
}
