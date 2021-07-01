package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModelProperty;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

public class AwsLoadBalancerResponse implements Serializable {

    @ApiModelProperty(StackModelDescription.AWS_LB_ARN)
    @NotNull
    private String arn;

    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }
}
