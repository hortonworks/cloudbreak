package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;

public class CoreVerticalScalePreparationRequest extends CloudStackRequest {

    private final StackDto stack;

    private final InstanceGroupDto instanceGroup;

    private final List<CloudResource> cloudResources;

    private final StackVerticalScaleV4Request stackVerticalScaleV4Request;

    public CoreVerticalScalePreparationRequest(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudStack") CloudStack cloudStack,
            @JsonProperty("stack") StackDto stack,
            @JsonProperty("instanceGroup") InstanceGroupDto instanceGroup,
            @JsonProperty("cloudResources") List<CloudResource> cloudResources,
            @JsonProperty("stackVerticalScaleV4Request") StackVerticalScaleV4Request stackVerticalScaleV4Request) {
        super(cloudContext, cloudCredential, cloudStack);
        this.stack = stack;
        this.instanceGroup = instanceGroup;
        this.cloudResources = cloudResources;
        this.stackVerticalScaleV4Request = stackVerticalScaleV4Request;
    }

    public StackVerticalScaleV4Request getStackVerticalScaleV4Request() {
        return stackVerticalScaleV4Request;
    }

    public InstanceGroupDto getInstanceGroup() {
        return instanceGroup;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    public StackDto getStack() {
        return stack;
    }

    public String toString() {
        return new StringJoiner(", ", CoreVerticalScalePreparationRequest.class.getSimpleName() + "[", "]")
                .add("stackDto=" + stack)
                .add("instanceGroup=" + instanceGroup)
                .add("cloudResources=" + cloudResources)
                .add("stackVerticalScaleV4Request=" + stackVerticalScaleV4Request)
                .toString();
    }
}
