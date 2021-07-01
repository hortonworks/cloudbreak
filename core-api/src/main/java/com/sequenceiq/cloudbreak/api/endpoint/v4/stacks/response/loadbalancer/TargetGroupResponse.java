package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer;

import java.io.Serializable;
import java.util.Set;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModelProperty;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class TargetGroupResponse implements Serializable {

    @ApiModelProperty(StackModelDescription.TARGET_GROUP_PORT)
    @NotNull
    private long port;

    @ApiModelProperty(StackModelDescription.TARGET_GROUP_INSTANCES)
    @NotNull
    private Set<String> targetInstances;

    @ApiModelProperty(StackModelDescription.TARGET_GROUP_AWS)
    private AwsTargetGroupResponse awsResourceIds;

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
}
