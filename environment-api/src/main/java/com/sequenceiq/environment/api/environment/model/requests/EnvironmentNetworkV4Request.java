package com.sequenceiq.environment.api.environment.model.requests;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.environment.doc.EnvironmentNetworkDescription;
import com.sequenceiq.environment.api.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.environment.model.EnvironmentNetworkAzureParams;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EnvironmentNetworkV4Request {
    @ApiModelProperty(value = EnvironmentNetworkDescription.SUBNET_IDS, required = true)
    private Set<String> subnetIds;

    @ApiModelProperty(EnvironmentNetworkDescription.AWS_SPECIFIC_PARAMETERS)
    private EnvironmentNetworkAwsParams aws;

    @ApiModelProperty(EnvironmentNetworkDescription.AZURE_SPECIFIC_PARAMETERS)
    private EnvironmentNetworkAzureParams azure;

    public Set<String> getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(Set<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    public EnvironmentNetworkAwsParams getAws() {
        return aws;
    }

    public void setAws(EnvironmentNetworkAwsParams aws) {
        this.aws = aws;
    }

    public EnvironmentNetworkAzureParams getAzure() {
        return azure;
    }

    public void setAzure(EnvironmentNetworkAzureParams azure) {
        this.azure = azure;
    }
}
