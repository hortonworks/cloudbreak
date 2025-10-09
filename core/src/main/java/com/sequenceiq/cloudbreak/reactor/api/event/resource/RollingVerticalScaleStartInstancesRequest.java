package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class RollingVerticalScaleStartInstancesRequest extends StackEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final List<CloudInstance> cloudInstances;

    private final List<CloudResource> cloudResources;

    private final RollingVerticalScaleResult rollingVerticalScaleResult;

    @JsonCreator
    public RollingVerticalScaleStartInstancesRequest(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudResources") List<CloudResource> cloudResources,
            @JsonProperty("cloudInstances") List<CloudInstance> cloudInstances,
            @JsonProperty("rollingVerticalScaleResult") RollingVerticalScaleResult rollingVerticalScaleResult) {
        super(EventSelectorUtil.selector(RollingVerticalScaleStartInstancesRequest.class), resourceId);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.cloudInstances = cloudInstances;
        this.cloudResources = cloudResources;
        this.rollingVerticalScaleResult = rollingVerticalScaleResult;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public List<CloudInstance> getCloudInstances() {
        return cloudInstances;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    public RollingVerticalScaleResult getRollingVerticalScaleResult() {
        return rollingVerticalScaleResult;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RollingVerticalScaleStartInstancesRequest.class.getSimpleName() + "[", "]")
                .add("cloudInstances=" + cloudInstances)
                .add("cloudResources=" + cloudResources)
                .add("rollingVerticalScaleResult=" + rollingVerticalScaleResult)
                .add(super.toString())
                .toString();
    }
}
