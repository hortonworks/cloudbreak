package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class UpdateClouderaManagerConfigRequest<T> extends CloudStackRequest<T> {

    private final StackVerticalScaleV4Request stackVerticalScaleV4Request;

    private final Set<String> yarnMountPaths;

    private final Set<String> impalaMountPaths;

    @JsonCreator
    public UpdateClouderaManagerConfigRequest(@JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("stack") CloudStack stack,
            @JsonProperty("yarnMountPaths") Set<String> yarnMountPaths,
            @JsonProperty("impalaMountPaths") Set<String> impalaMountPaths,
            @JsonProperty("stackVerticalScaleV4Request") StackVerticalScaleV4Request stackVerticalScaleV4Request) {
        super(cloudContext, cloudCredential, stack);
        this.yarnMountPaths = yarnMountPaths;
        this.impalaMountPaths = impalaMountPaths;
        this.stackVerticalScaleV4Request = stackVerticalScaleV4Request;
    }

    public StackVerticalScaleV4Request getStackVerticalScaleV4Request() {
        return stackVerticalScaleV4Request;
    }

    public Set<String> getYarnMountPaths() {
        return yarnMountPaths;
    }

    public Set<String> getImpalaMountPaths() {
        return impalaMountPaths;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UpdateClouderaManagerConfigRequest.class.getSimpleName() + "[", "]")
                .add("yarnMountPaths=" + yarnMountPaths)
                .add("impalaMountPaths=" + impalaMountPaths)
                .add("stackVerticalScaleV4Request=" + stackVerticalScaleV4Request)
                .toString();
    }
}
