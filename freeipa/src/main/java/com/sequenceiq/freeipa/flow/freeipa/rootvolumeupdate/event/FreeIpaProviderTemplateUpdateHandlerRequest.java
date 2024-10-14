package com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaProviderTemplateUpdateHandlerRequest extends StackEvent {

    private final String operationId;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final CloudStack cloudStack;

    @JsonCreator
    public FreeIpaProviderTemplateUpdateHandlerRequest(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudStack") CloudStack cloudStack) {
        super(selector, stackId);
        this.operationId = operationId;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.cloudStack = cloudStack;
    }

    public String getOperationId() {
        return operationId;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    public String toString() {
        return new StringJoiner(", ", FreeIpaProviderTemplateUpdateHandlerRequest.class.getSimpleName() + "[", "]")
                .add("selector=" + super.getSelector())
                .add("stackId=" + super.getResourceId())
                .add("operationId=" + operationId)
                .toString();
    }
}
