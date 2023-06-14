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

public class CoreVerticalScaleRequest<T> extends CloudStackRequest<T> {

    private final List<CloudResource> resourceList;

    private final StackVerticalScaleV4Request stackVerticalScaleV4Request;

    public CoreVerticalScaleRequest(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("stack") CloudStack stack,
            @JsonProperty("resourceList") List<CloudResource> resourceList,
            @JsonProperty("stackVerticalScaleV4Request") StackVerticalScaleV4Request stackVerticalScaleV4Request) {
        super(cloudContext, cloudCredential, stack);
        this.resourceList = resourceList;
        this.stackVerticalScaleV4Request = stackVerticalScaleV4Request;
    }

    public List<CloudResource> getResourceList() {
        return resourceList;
    }

    public StackVerticalScaleV4Request getStackVerticalScaleV4Request() {
        return stackVerticalScaleV4Request;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CoreVerticalScaleRequest.class.getSimpleName() + "[", "]")
                .add("resourceList=" + resourceList)
                .toString();
    }
}
