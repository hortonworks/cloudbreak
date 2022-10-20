package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class StackCreationContext extends CommonContext {

    private final ProvisionType provisionType;

    private final StackView stack;

    private final String cloudPlatform;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    public StackCreationContext(FlowParameters flowParameters, StackView stack, String cloudPlatform, CloudContext cloudContext,
                                CloudCredential cloudCredential, ProvisionType provisionType) {
        super(flowParameters);
        this.provisionType = provisionType;
        this.stack = stack;
        this.cloudPlatform = cloudPlatform;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
    }

    public StackCreationContext(FlowParameters flowParameters, StackView stack, String cloudPlatform, CloudContext cloudContext,
                                CloudCredential cloudCredential) {
        super(flowParameters);
        this.provisionType = ProvisionType.REGULAR;
        this.stack = stack;
        this.cloudPlatform = cloudPlatform;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
    }

    public ProvisionType getProvisionType() {
        return provisionType;
    }

    public Long getStackId() {
        return stack.getId();
    }

    public StackView getStack() {
        return stack;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }
}
