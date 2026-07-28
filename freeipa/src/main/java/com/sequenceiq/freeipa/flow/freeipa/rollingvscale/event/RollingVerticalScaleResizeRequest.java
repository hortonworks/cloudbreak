package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RollingVerticalScaleResizeRequest extends StackEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final CloudStack cloudStack;

    private final List<CloudResource> cloudResources;

    private final String group;

    @JsonCreator
    public RollingVerticalScaleResizeRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudStack") CloudStack cloudStack,
            @JsonProperty("cloudResources") List<CloudResource> cloudResources,
            @JsonProperty("group") String group) {
        super(stackId);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.cloudStack = cloudStack;
        this.cloudResources = cloudResources;
        this.group = group;
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

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    public String getGroup() {
        return group;
    }

    @Override
    public String toString() {
        return "RollingVerticalScaleResizeRequest{" +
                "cloudContext=" + cloudContext +
                ", group='" + group + '\'' +
                "} " + super.toString();
    }
}
