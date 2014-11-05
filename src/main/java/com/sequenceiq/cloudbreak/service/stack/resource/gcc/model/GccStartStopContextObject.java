package com.sequenceiq.cloudbreak.service.stack.resource.gcc.model;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.resource.StartStopContextObject;

public class GccStartStopContextObject extends StartStopContextObject {
    public GccStartStopContextObject(Stack stack) {
        super(stack);
    }
}
