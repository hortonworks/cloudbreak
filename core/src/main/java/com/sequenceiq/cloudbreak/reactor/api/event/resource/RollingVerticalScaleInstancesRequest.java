package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class RollingVerticalScaleInstancesRequest extends StackEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final CloudStack cloudStack;

    private final List<CloudResource> cloudResources;

    private final StackVerticalScaleV4Request stackVerticalScaleV4Request;

    private final RollingVerticalScaleResult rollingVerticalScaleResult;

    @JsonCreator
    public RollingVerticalScaleInstancesRequest(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudStack") CloudStack cloudStack,
            @JsonProperty("cloudResources") List<CloudResource> cloudResources,
            @JsonProperty("stackVerticalScaleV4Request") StackVerticalScaleV4Request stackVerticalScaleV4Request,
            @JsonProperty("rollingVerticalScaleResult") RollingVerticalScaleResult rollingVerticalScaleResult) {
        super(EventSelectorUtil.selector(RollingVerticalScaleInstancesRequest.class), resourceId);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.cloudStack = cloudStack;
        this.cloudResources = cloudResources;
        this.stackVerticalScaleV4Request = stackVerticalScaleV4Request;
        this.rollingVerticalScaleResult = rollingVerticalScaleResult;
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

    public StackVerticalScaleV4Request getStackVerticalScaleV4Request() {
        return stackVerticalScaleV4Request;
    }

    public RollingVerticalScaleResult getRollingVerticalScaleResult() {
        return rollingVerticalScaleResult;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RollingVerticalScaleInstancesRequest.class.getSimpleName() + "[", "]")
                .add("cloudResources=" + cloudResources)
                .add("stackVerticalScaleV4Request=" + stackVerticalScaleV4Request)
                .add("rollingVerticalScaleResult=" + rollingVerticalScaleResult)
                .add(super.toString())
                .toString();
    }
}
