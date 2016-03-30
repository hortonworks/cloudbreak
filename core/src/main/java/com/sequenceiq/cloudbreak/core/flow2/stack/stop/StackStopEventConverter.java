package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import com.sequenceiq.cloudbreak.core.flow2.EventConverterAdapter;

public class StackStopEventConverter extends EventConverterAdapter<StackStopEvent> {
    public StackStopEventConverter() {
        super(StackStopEvent.class);
    }
}
