package com.sequenceiq.distrox.api.v1.distrox.model.network;

import java.io.Serializable;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.NetworkModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class NetworkV1Base implements Serializable {

    @ApiModelProperty(NetworkModelDescription.AWS_PARAMETERS)
    private AwsNetworkV1Parameters aws;

    @ApiModelProperty(NetworkModelDescription.AZURE_PARAMETERS)
    private AzureNetworkV1Parameters azure;

    public void setAws(AwsNetworkV1Parameters aws) {
        this.aws = aws;
    }

    public void setAzure(AzureNetworkV1Parameters azure) {
        this.azure = azure;
    }

    public AwsNetworkV1Parameters getAws() {
        return aws;
    }

    public AzureNetworkV1Parameters getAzure() {
        return azure;
    }
}