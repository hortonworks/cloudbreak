package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModelProperty;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

public class AwsTargetGroupResponse implements Serializable {

    @ApiModelProperty(StackModelDescription.AWS_LISTENER_ARN)
    @NotNull
    private String listenerArn;

    @ApiModelProperty(StackModelDescription.AWS_TARGETGROUP_ARN)
    @NotNull
    private String targetGroupArn;

    public String getListenerArn() {
        return listenerArn;
    }

    public void setListenerArn(String listenerArn) {
        this.listenerArn = listenerArn;
    }

    public String getTargetGroupArn() {
        return targetGroupArn;
    }

    public void setTargetGroupArn(String targetGroupArn) {
        this.targetGroupArn = targetGroupArn;
    }
}
