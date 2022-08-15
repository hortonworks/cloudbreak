package com.sequenceiq.freeipa.flow.freeipa.verticalscale.event;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;

public class FreeIpaVerticalScaleRequest extends CloudStackRequest<FreeIpaVerticalScaleResult> {

    private final List<CloudResource> resourceList;

    private final VerticalScaleRequest freeIPAVerticalScaleRequest;

    public FreeIpaVerticalScaleRequest(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("stack") CloudStack stack,
            @JsonProperty("resourceList") List<CloudResource> resourceList,
            @JsonProperty("freeIPAVerticalScaleRequest")
                    VerticalScaleRequest freeIPAVerticalScaleRequest) {
        super(cloudContext, cloudCredential, stack);
        this.resourceList = resourceList;
        this.freeIPAVerticalScaleRequest = freeIPAVerticalScaleRequest;
    }

    public List<CloudResource> getResourceList() {
        return resourceList;
    }

    public VerticalScaleRequest getFreeIPAVerticalScaleRequest() {
        return freeIPAVerticalScaleRequest;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FreeIpaVerticalScaleRequest.class.getSimpleName() + "[", "]")
                .add("resourceList=" + resourceList)
                .add(super.toString())
                .toString();
    }
}
