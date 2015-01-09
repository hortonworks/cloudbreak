package com.sequenceiq.cloudbreak.service.stack.flow.dummy;

import com.sequenceiq.cloudbreak.service.stack.resource.DescribeContextObject;

public class DummyDescribeContextObject extends DescribeContextObject {
    protected DummyDescribeContextObject(Long stackId) {
        super(stackId);
    }
}
