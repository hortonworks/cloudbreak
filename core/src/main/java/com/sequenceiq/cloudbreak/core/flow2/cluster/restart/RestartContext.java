package com.sequenceiq.cloudbreak.core.flow2.cluster.restart;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class RestartContext extends CommonContext {

    private final Stack stack;

    private final List<String> instanceIds;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    public RestartContext(FlowParameters flowParameters, Stack stack, List<String> instanceIds,
            CloudContext cloudContext, CloudCredential cloudCredential) {
        super(flowParameters);
        this.stack = stack;
        this.instanceIds = instanceIds;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
    }

    public Stack getStack() {
        return stack;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }
}
