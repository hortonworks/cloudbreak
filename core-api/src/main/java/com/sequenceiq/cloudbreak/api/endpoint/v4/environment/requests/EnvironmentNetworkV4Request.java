package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.base.EnvironmentNetworkAwsV4Params;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.base.EnvironmentNetworkAzureV4Params;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnvironmentNetworkV4Request {
    @ApiModelProperty(value = ModelDescriptions.EnvironmentNetworkDescription.SUBNET_IDS)
    private Set<String> subnetIds;

    @ApiModelProperty(value = ModelDescriptions.EnvironmentNetworkDescription.NETWORK_CIDR)
    private String networkCidr;

    @ApiModelProperty(value = ModelDescriptions.EnvironmentNetworkDescription.AWS_SPECIFIC_PARAMETERS)
    private EnvironmentNetworkAwsV4Params aws;

    @ApiModelProperty(value = ModelDescriptions.EnvironmentNetworkDescription.AZURE_SPECIFIC_PARAMETERS)
    private EnvironmentNetworkAzureV4Params azure;

    public Set<String> getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(Set<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    public EnvironmentNetworkAwsV4Params getAws() {
        return aws;
    }

    public void setAws(EnvironmentNetworkAwsV4Params aws) {
        this.aws = aws;
    }

    public EnvironmentNetworkAzureV4Params getAzure() {
        return azure;
    }

    public void setAzure(EnvironmentNetworkAzureV4Params azure) {
        this.azure = azure;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }
}
