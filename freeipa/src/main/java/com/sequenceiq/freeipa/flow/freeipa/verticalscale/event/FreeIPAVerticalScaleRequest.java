package com.sequenceiq.freeipa.flow.freeipa.verticalscale.event;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class FreeIPAVerticalScaleRequest<T> extends CloudStackRequest<T> {

    private final List<CloudResource> resourceList;

    private final com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.FreeIPAVerticalScaleRequest freeIPAVerticalScaleRequest;

    public FreeIPAVerticalScaleRequest(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("stack") CloudStack stack,
            @JsonProperty("resourceList") List<CloudResource> resourceList,
            @JsonProperty("freeIPAVerticalScaleRequest")
                    com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.FreeIPAVerticalScaleRequest freeIPAVerticalScaleRequest) {
        super(cloudContext, cloudCredential, stack);
        this.resourceList = resourceList;
        this.freeIPAVerticalScaleRequest = freeIPAVerticalScaleRequest;
    }

    public List<CloudResource> getResourceList() {
        return resourceList;
    }

    public com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.FreeIPAVerticalScaleRequest getFreeIPAVerticalScaleRequest() {
        return freeIPAVerticalScaleRequest;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FreeIPAVerticalScaleRequest.class.getSimpleName() + "[", "]")
                .add("resourceList=" + resourceList)
                .toString();
    }
}
