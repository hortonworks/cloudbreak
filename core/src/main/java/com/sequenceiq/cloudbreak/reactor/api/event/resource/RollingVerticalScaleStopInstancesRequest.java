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

public class RollingVerticalScaleStopInstancesRequest extends StackEvent {

    private final CloudCredential cloudCredential;

    private final CloudContext cloudContext;

    private final List<CloudInstance> cloudInstances;

    private final List<CloudResource> cloudResources;

    private final String targetInstanceType;

    private final RollingVerticalScaleResult rollingVerticalScaleResult;

    @JsonCreator
    public RollingVerticalScaleStopInstancesRequest(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudResources") List<CloudResource> cloudResources,
            @JsonProperty("cloudInstances") List<CloudInstance> cloudInstances,
            @JsonProperty("targetInstanceType") String targetInstanceType,
            @JsonProperty("rollingVerticalScaleResult") RollingVerticalScaleResult rollingVerticalScaleResult) {
        super(EventSelectorUtil.selector(RollingVerticalScaleStopInstancesRequest.class), resourceId);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.cloudInstances = cloudInstances;
        this.cloudResources = cloudResources;
        this.targetInstanceType = targetInstanceType;
        this.rollingVerticalScaleResult = rollingVerticalScaleResult;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public List<CloudInstance> getCloudInstances() {
        return cloudInstances;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    public String getTargetInstanceType() {
        return targetInstanceType;
    }

    public RollingVerticalScaleResult getRollingVerticalScaleResult() {
        return rollingVerticalScaleResult;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RollingVerticalScaleStopInstancesRequest.class.getSimpleName() + "[", "]")
                .add("cloudInstances=" + cloudInstances)
                .add("cloudResources=" + cloudResources)
                .add("targetInstanceType=" + targetInstanceType)
                .add("rollingVerticalScaleResult=" + rollingVerticalScaleResult)
                .add(super.toString())
                .toString();
    }
}
