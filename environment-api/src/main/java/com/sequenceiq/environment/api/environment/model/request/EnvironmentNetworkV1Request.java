package com.sequenceiq.environment.api.environment.model.request;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;
import com.sequenceiq.environment.api.environment.model.EnvironmentNetworkAwsV1Params;
import com.sequenceiq.environment.api.environment.model.EnvironmentNetworkAzureV1Params;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EnvironmentNetworkV1Request {
    @ApiModelProperty(value = EnvironmentModelDescription.SUBNET_IDS, required = true)
    private Set<String> subnetIds;

    @ApiModelProperty(EnvironmentModelDescription.AWS_SPECIFIC_PARAMETERS)
    private EnvironmentNetworkAwsV1Params aws;

    @ApiModelProperty(EnvironmentModelDescription.AZURE_SPECIFIC_PARAMETERS)
    private EnvironmentNetworkAzureV1Params azure;

    public Set<String> getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(Set<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    public EnvironmentNetworkAwsV1Params getAws() {
        return aws;
    }

    public void setAws(EnvironmentNetworkAwsV1Params aws) {
        this.aws = aws;
    }

    public EnvironmentNetworkAzureV1Params getAzure() {
        return azure;
    }

    public void setAzure(EnvironmentNetworkAzureV1Params azure) {
        this.azure = azure;
    }
}
