package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class ValidateDiskRequest<T> extends CloudPlatformRequest<ValidateDiskResult> {

    private final CloudStack stack;

    private final List<CloudResource> cloudResources;

    @JsonCreator
    public ValidateDiskRequest(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("stack") CloudStack stack,
            @JsonProperty("cloudResources") List<CloudResource> cloudResources) {
        super(cloudContext, cloudCredential);
        this.stack = stack;
        this.cloudResources = cloudResources;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    public CloudStack getStack() {
        return stack;
    }

    @Override
    public String toString() {
        return "ValidateDiskRequest{" +
                ", stack=" + stack +
                ", cloudResources=" + cloudResources +
                "} " + super.toString();
    }
}
