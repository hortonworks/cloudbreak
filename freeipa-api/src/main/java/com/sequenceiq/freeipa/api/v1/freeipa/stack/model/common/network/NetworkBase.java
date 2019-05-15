package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.NetworkModelDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class NetworkBase {
    @ApiModelProperty(NetworkModelDescription.AWS_PARAMETERS)
    private AwsNetworkParameters aws;

    @ApiModelProperty(NetworkModelDescription.AZURE_PARAMETERS)
    private AzureNetworkParameters azure;

    public void setAws(AwsNetworkParameters aws) {
        this.aws = aws;
    }

    public void setAzure(AzureNetworkParameters azure) {
        this.azure = azure;
    }

    public AwsNetworkParameters getAws() {
        return aws;
    }

    public AzureNetworkParameters getAzure() {
        return azure;
    }
}
