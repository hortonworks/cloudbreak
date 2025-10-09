package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class RollingVerticalScaleContext extends CommonContext {

    private final Stack stack;

    private final List<String> instanceIds;

    private final StackVerticalScaleV4Request stackVerticalScaleV4Request;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final String targetInstanceType;

    public RollingVerticalScaleContext(FlowParameters flowParameters, Stack stack, List<String> instanceIds,
            StackVerticalScaleV4Request request, CloudContext cloudContext, CloudCredential cloudCredential, String targetInstanceType) {
        super(flowParameters);
        this.stack = stack;
        this.instanceIds = instanceIds;
        this.stackVerticalScaleV4Request = request;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.targetInstanceType = targetInstanceType;
    }

    public Stack getStack() {
        return stack;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    public StackVerticalScaleV4Request getStackVerticalScaleV4Request() {
        return stackVerticalScaleV4Request;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public String getTargetInstanceType() {
        return targetInstanceType;
    }
}
