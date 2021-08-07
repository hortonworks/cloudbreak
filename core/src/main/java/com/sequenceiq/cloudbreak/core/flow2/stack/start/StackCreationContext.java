package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.flow.core.FlowParameters;

public class StackCreationContext extends StackContext {

    private final ProvisionType provisionType;

    public StackCreationContext(FlowParameters flowParameters, Stack stack, CloudContext cloudContext,
            CloudCredential cloudCredential, CloudStack cloudStack, ProvisionType provisionType) {
        super(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
        this.provisionType = provisionType;
    }

    public StackCreationContext(FlowParameters flowParameters, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack) {
        super(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
        this.provisionType = ProvisionType.REGULAR;
    }

    public ProvisionType getProvisionType() {
        return provisionType;
    }
}
