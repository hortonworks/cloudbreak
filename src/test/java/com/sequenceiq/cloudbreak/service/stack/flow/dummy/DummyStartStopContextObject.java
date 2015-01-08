package com.sequenceiq.cloudbreak.service.stack.flow.dummy;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.resource.StartStopContextObject;

public class DummyStartStopContextObject extends StartStopContextObject {

    protected DummyStartStopContextObject(Stack stack) {
        super(stack);
    }
}
