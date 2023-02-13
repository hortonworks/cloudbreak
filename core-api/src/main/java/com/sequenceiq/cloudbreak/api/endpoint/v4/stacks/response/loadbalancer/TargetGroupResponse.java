package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer;

import java.io.Serializable;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_NULL)
public class TargetGroupResponse implements Serializable {

    @Schema(description = StackModelDescription.TARGET_GROUP_PORT)
    @NotNull
    private long port;

    @Schema(description = StackModelDescription.TARGET_GROUP_INSTANCES)
    @NotNull
    private Set<String> targetInstances;

    @Schema(description = StackModelDescription.TARGET_GROUP_AWS)
    private AwsTargetGroupResponse awsResourceIds;

    @Schema(description = StackModelDescription.TARGET_GROUP_AZURE)
    private AzureTargetGroupResponse azureResourceId;

    @Schema
    private GcpTargetGroupResponse gcpResourceId;

    public long getPort() {
        return port;
    }

    public void setPort(long port) {
        this.port = port;
    }

    public Set<String> getTargetInstances() {
        return targetInstances;
    }

    public void setTargetInstances(Set<String> targetInstances) {
        this.targetInstances = targetInstances;
    }

    public AwsTargetGroupResponse getAwsResourceIds() {
        return awsResourceIds;
    }

    public void setAwsResourceIds(AwsTargetGroupResponse awsResourceIds) {
        this.awsResourceIds = awsResourceIds;
    }

    public AzureTargetGroupResponse getAzureResourceId() {
        return azureResourceId;
    }

    public void setAzureResourceId(AzureTargetGroupResponse azureResourceId) {
        this.azureResourceId = azureResourceId;
    }

    public GcpTargetGroupResponse getGcpResourceId() {
        return gcpResourceId;
    }

    public void setGcpResourceId(GcpTargetGroupResponse gcpResourceId) {
        this.gcpResourceId = gcpResourceId;
    }
}
