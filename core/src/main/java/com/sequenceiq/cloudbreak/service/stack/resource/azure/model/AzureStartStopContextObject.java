package com.sequenceiq.cloudbreak.service.stack.resource.azure.model;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.resource.StartStopContextObject;

public class AzureStartStopContextObject extends StartStopContextObject {

    public AzureStartStopContextObject(Stack stack) {
        super(stack);
    }

}
