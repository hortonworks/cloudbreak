package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.api.type.LoadBalancerType;

@JsonInclude(Include.NON_NULL)
public class LoadBalancerResponse implements Serializable {

    @ApiModelProperty
    private String fqdn;

    @ApiModelProperty
    private String cloudDns;

    @ApiModelProperty
    private String ip;

    @ApiModelProperty
    @NotNull
    private List<TargetGroupResponse> targets;

    @ApiModelProperty
    @NotNull
    private LoadBalancerType type;

    // null for non-AWS stacks
    @ApiModelProperty
    private AwsLoadBalancerResponse awsResourceId;

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public String getCloudDns() {
        return cloudDns;
    }

    public void setCloudDns(String cloudDns) {
        this.cloudDns = cloudDns;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public List<TargetGroupResponse> getTargets() {
        return targets;
    }

    public void setTargets(List<TargetGroupResponse> targets) {
        this.targets = targets;
    }

    public LoadBalancerType getType() {
        return type;
    }

    public void setType(LoadBalancerType type) {
        this.type = type;
    }

    public AwsLoadBalancerResponse getAwsResourceId() {
        return awsResourceId;
    }

    public void setAwsResourceId(AwsLoadBalancerResponse awsResourceId) {
        this.awsResourceId = awsResourceId;
    }
}
