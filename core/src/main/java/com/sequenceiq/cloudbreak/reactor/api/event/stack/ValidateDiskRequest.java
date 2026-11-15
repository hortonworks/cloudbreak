package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import java.util.List;
import java.util.Set;

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

    private final List<CloudResource> embeddedDatabaseVolumes;

    private final Set<String> clusterManagerServerWithEmbeddedDbInstanceIds;

    @JsonCreator
    public ValidateDiskRequest(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("stack") CloudStack stack,
            @JsonProperty("cloudResources") List<CloudResource> cloudResources,
            @JsonProperty("embeddedDatabaseVolumes") List<CloudResource> embeddedDatabaseVolumes,
            @JsonProperty("clusterManagerServerWithEmbeddedDbInstanceIds") Set<String> clusterManagerServerWithEmbeddedDbInstanceIds) {
        super(cloudContext, cloudCredential);
        this.stack = stack;
        this.cloudResources = cloudResources;
        this.embeddedDatabaseVolumes = embeddedDatabaseVolumes == null ? List.of() : embeddedDatabaseVolumes;
        this.clusterManagerServerWithEmbeddedDbInstanceIds = clusterManagerServerWithEmbeddedDbInstanceIds == null
                ? Set.of() : clusterManagerServerWithEmbeddedDbInstanceIds;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    public List<CloudResource> getEmbeddedDatabaseVolumes() {
        return embeddedDatabaseVolumes;
    }

    public Set<String> getClusterManagerServerWithEmbeddedDbInstanceIds() {
        return clusterManagerServerWithEmbeddedDbInstanceIds;
    }

    public CloudStack getStack() {
        return stack;
    }

    @Override
    public String toString() {
        return "ValidateDiskRequest{" +
                ", stack=" + stack +
                ", cloudResources=" + cloudResources +
                ", embeddedDatabaseVolumes=" + embeddedDatabaseVolumes +
                ", clusterManagerServerWithEmbeddedDbInstanceIds=" + clusterManagerServerWithEmbeddedDbInstanceIds +
                "} " + super.toString();
    }
}
