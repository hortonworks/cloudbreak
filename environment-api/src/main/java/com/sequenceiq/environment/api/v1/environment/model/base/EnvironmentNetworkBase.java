package com.sequenceiq.environment.api.v1.environment.model.base;

import java.util.Set;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(subTypes = {EnvironmentNetworkRequest.class, EnvironmentNetworkResponse.class})
public abstract class EnvironmentNetworkBase {

    @ApiModelProperty(value = EnvironmentModelDescription.SUBNET_IDS)
    private Set<String> subnetIds;

    @Size(max = 255)
    private String networkCidr;

    @ApiModelProperty(EnvironmentModelDescription.PRIVATE_SUBNET_CREATION)
    private PrivateSubnetCreation privateSubnetCreation = PrivateSubnetCreation.DISABLED;

    @ApiModelProperty(EnvironmentModelDescription.AWS_SPECIFIC_PARAMETERS)
    private EnvironmentNetworkAwsParams aws;

    @ApiModelProperty(EnvironmentModelDescription.AZURE_SPECIFIC_PARAMETERS)
    private EnvironmentNetworkAzureParams azure;

    @ApiModelProperty(EnvironmentModelDescription.YARN_SPECIFIC_PARAMETERS)
    private EnvironmentNetworkYarnParams yarn;

    public Set<String> getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(Set<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }

    public PrivateSubnetCreation getPrivateSubnetCreation() {
        return privateSubnetCreation;
    }

    public void setPrivateSubnetCreation(PrivateSubnetCreation privateSubnetCreation) {
        this.privateSubnetCreation = privateSubnetCreation;
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

    public EnvironmentNetworkYarnParams getYarn() {
        return yarn;
    }

    public void setYarn(EnvironmentNetworkYarnParams yarn) {
        this.yarn = yarn;
    }
}
