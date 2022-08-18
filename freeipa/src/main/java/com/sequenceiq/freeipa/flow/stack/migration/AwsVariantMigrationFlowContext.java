package com.sequenceiq.freeipa.flow.stack.migration;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;

public class AwsVariantMigrationFlowContext extends StackContext {

    public AwsVariantMigrationFlowContext(FlowParameters flowParameters, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential,
            CloudStack cloudStack) {
        super(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }
}
