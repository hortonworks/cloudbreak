package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.base.EnvironmentNetworkAwsV4Params;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.base.EnvironmentNetworkAzureV4Params;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

public class EnvironmentNetworkV4Request {
    @ApiModelProperty(value = ModelDescriptions.EnvironmentNetworkDescription.SUBNET_IDS, required = true)
    private Set<String> subnetIds;

    @ApiModelProperty(value = ModelDescriptions.EnvironmentNetworkDescription.AWS_SPECIFIC_PARAMETERS, required = true)
    private EnvironmentNetworkAwsV4Params aws;

    @ApiModelProperty(value = ModelDescriptions.EnvironmentNetworkDescription.AZURE_SPECIFIC_PARAMETERS, required = true)
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
}
