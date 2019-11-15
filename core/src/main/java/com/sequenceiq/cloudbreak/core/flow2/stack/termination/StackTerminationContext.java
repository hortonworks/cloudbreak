package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.flow.core.FlowParameters;

public class StackTerminationContext extends StackContext {

    private final List<CloudResource> cloudResources;

    private final boolean terminationForced;

    public StackTerminationContext(FlowParameters flowParameters, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack,
            List<CloudResource> cloudResources, boolean terminationForced) {
        super(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
        this.cloudResources = cloudResources;
        this.terminationForced = terminationForced;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    public boolean getTerminationForced() {
        return terminationForced;
    }
}
