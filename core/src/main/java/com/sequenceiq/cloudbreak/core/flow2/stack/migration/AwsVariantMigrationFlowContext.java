package com.sequenceiq.cloudbreak.core.flow2.stack.migration;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.flow.core.FlowParameters;

public class AwsVariantMigrationFlowContext extends StackContext {

    public AwsVariantMigrationFlowContext(FlowParameters flowParameters, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential,
            CloudStack cloudStack) {
        super(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }
}
