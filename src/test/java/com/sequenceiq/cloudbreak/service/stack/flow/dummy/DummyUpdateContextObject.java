package com.sequenceiq.cloudbreak.service.stack.flow.dummy;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.resource.UpdateContextObject;

public class DummyUpdateContextObject extends UpdateContextObject {
    protected DummyUpdateContextObject(Stack stack) {
        super(stack);
    }
}
