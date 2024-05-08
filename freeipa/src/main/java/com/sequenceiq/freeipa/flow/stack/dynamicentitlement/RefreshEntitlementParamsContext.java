package com.sequenceiq.freeipa.flow.stack.dynamicentitlement;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;

public class RefreshEntitlementParamsContext extends StackContext {

    public RefreshEntitlementParamsContext(FlowParameters flowParameters, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential,
            CloudStack cloudStack) {
        super(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

}

