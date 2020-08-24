package com.sequenceiq.distrox.api.v1.distrox.model.network;

import java.io.Serializable;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.NetworkModelDescription;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.InstanceGroupAwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.azure.InstanceGroupAzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.mock.InstanceGroupMockNetworkV1Parameters;

import io.swagger.annotations.ApiModelProperty;

public class InstanceGroupNetworkV1Base implements Serializable {

    @ApiModelProperty(NetworkModelDescription.AWS_PARAMETERS)
    private InstanceGroupAwsNetworkV1Parameters aws;

    @ApiModelProperty(NetworkModelDescription.AZURE_PARAMETERS)
    private InstanceGroupAzureNetworkV1Parameters azure;

    @ApiModelProperty(NetworkModelDescription.MOCK_PARAMETERS)
    private InstanceGroupMockNetworkV1Parameters mock;

    public void setAws(InstanceGroupAwsNetworkV1Parameters aws) {
        this.aws = aws;
    }

    public void setAzure(InstanceGroupAzureNetworkV1Parameters azure) {
        this.azure = azure;
    }

    public InstanceGroupAwsNetworkV1Parameters getAws() {
        return aws;
    }

    public InstanceGroupAzureNetworkV1Parameters getAzure() {
        return azure;
    }

    public InstanceGroupMockNetworkV1Parameters getMock() {
        return mock;
    }

    public void setMock(InstanceGroupMockNetworkV1Parameters mock) {
        this.mock = mock;
    }

}