package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.api.type.LoadBalancerType;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_NULL)
public class LoadBalancerResponse implements Serializable {

    @Schema(description = StackModelDescription.LOAD_BALANCER_FQDN)
    private String fqdn;

    @Schema(description = StackModelDescription.LOAD_BALANCER_CLOUD_DNS)
    private String cloudDns;

    @Schema(description = StackModelDescription.LOAD_BALANCER_IP)
    private String ip;

    @Schema(description = StackModelDescription.LOAD_BALANCER_TARGETS, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private List<TargetGroupResponse> targets = new ArrayList<>();

    @Schema(description = StackModelDescription.LOAD_BALANCER_TYPE, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private LoadBalancerType type;

    @Schema(description = StackModelDescription.LOAD_BALANCER_AWS)
    private AwsLoadBalancerResponse awsResourceId;

    @Schema(description = StackModelDescription.LOAD_BALANCER_AZURE)
    private AzureLoadBalancerResponse azureResourceId;

    @Schema(description = StackModelDescription.LOAD_BALANCER_GCP)
    private GcpLoadBalancerResponse gcpResourceId;

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

    public AzureLoadBalancerResponse getAzureResourceId() {
        return azureResourceId;
    }

    public void setAzureResourceId(AzureLoadBalancerResponse azureResourceId) {
        this.azureResourceId = azureResourceId;
    }

    public GcpLoadBalancerResponse getGcpResourceId() {
        return gcpResourceId;
    }

    public void setGcpResourceId(GcpLoadBalancerResponse gcpResourceId) {
        this.gcpResourceId = gcpResourceId;
    }
}
