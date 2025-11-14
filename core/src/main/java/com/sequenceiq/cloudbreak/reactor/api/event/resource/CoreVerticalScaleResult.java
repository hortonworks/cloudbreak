package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class CoreVerticalScaleResult extends CloudPlatformResult implements FlowPayload {

    private final ResourceStatus resourceStatus;

    private final List<CloudResourceStatus> results;

    private final StackVerticalScaleV4Request stackVerticalScaleV4Request;

    private final Integer instanceStorageCount;

    private final Integer instanceStorageSize;

    @JsonCreator
    public CoreVerticalScaleResult(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceStatus") ResourceStatus resourceStatus,
            @JsonProperty("results") List<CloudResourceStatus> results,
            @JsonProperty("stackVerticalScaleV4Request") StackVerticalScaleV4Request stackVerticalScaleV4Request,
            @JsonProperty("instanceStorageCount") Integer instanceStorageCount,
            @JsonProperty("instanceStorageSize") Integer instanceStorageSize) {
        super(resourceId);
        this.resourceStatus = resourceStatus;
        this.results = results;
        this.stackVerticalScaleV4Request = stackVerticalScaleV4Request;
        this.instanceStorageCount = instanceStorageCount;
        this.instanceStorageSize = instanceStorageSize;
    }

    public CoreVerticalScaleResult(String statusReason, Exception errorDetails, Long resourceId,
            StackVerticalScaleV4Request stackVerticalScaleV4Request) {
        super(statusReason, errorDetails, resourceId);
        this.resourceStatus = ResourceStatus.FAILED;
        this.stackVerticalScaleV4Request = stackVerticalScaleV4Request;
        this.results = new ArrayList<>();
        this.instanceStorageCount = 0;
        this.instanceStorageSize = 0;
    }

    public List<CloudResourceStatus> getResults() {
        return results;
    }

    public ResourceStatus getResourceStatus() {
        return resourceStatus;
    }

    public boolean isFailed() {
        return resourceStatus == ResourceStatus.FAILED;
    }

    public StackVerticalScaleV4Request getStackVerticalScaleV4Request() {
        return stackVerticalScaleV4Request;
    }

    public Integer getInstanceStorageCount() {
        return instanceStorageCount;
    }

    public Integer getInstanceStorageSize() {
        return instanceStorageSize;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CoreVerticalScaleResult.class.getSimpleName() + "[", "]")
                .add("results=" + results)
                .add("stackVerticalScaleV4Request=" + stackVerticalScaleV4Request)
                .add("instanceStorageCount=" + instanceStorageCount)
                .add("instanceStorageSize=" + instanceStorageSize)
                .toString();
    }
}