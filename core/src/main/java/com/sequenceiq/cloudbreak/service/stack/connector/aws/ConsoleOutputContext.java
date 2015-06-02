package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class ConsoleOutputContext extends StackContext {

    private String instanceId;

    public ConsoleOutputContext(Stack stack, String instanceId) {
        super(stack);
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
