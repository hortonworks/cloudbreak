package com.sequenceiq.distrox.api.v1.distrox.model.network;

import java.io.Serializable;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.NetworkModelDescription;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.AwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.azure.AzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.gcp.GcpNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.mock.MockNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.openstack.OpenstackNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.yarn.YarnNetworkV1Parameters;

import io.swagger.annotations.ApiModelProperty;

public class NetworkV1Base implements Serializable {

    @ApiModelProperty(NetworkModelDescription.AWS_PARAMETERS)
    private AwsNetworkV1Parameters aws;

    @ApiModelProperty(NetworkModelDescription.AZURE_PARAMETERS)
    private AzureNetworkV1Parameters azure;

    @ApiModelProperty(NetworkModelDescription.MOCK_PARAMETERS)
    private MockNetworkV1Parameters mock;

    @ApiModelProperty(NetworkModelDescription.GCP_PARAMETERS)
    private GcpNetworkV1Parameters gcp;

    @ApiModelProperty(NetworkModelDescription.YARN_PARAMETERS)
    private YarnNetworkV1Parameters yarn;

    @ApiModelProperty(NetworkModelDescription.OPEN_STACK_PARAMETERS)
    private OpenstackNetworkV1Parameters openstack;

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

    public MockNetworkV1Parameters getMock() {
        return mock;
    }

    public void setMock(MockNetworkV1Parameters mock) {
        this.mock = mock;
    }

    public GcpNetworkV1Parameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpNetworkV1Parameters gcp) {
        this.gcp = gcp;
    }

    public YarnNetworkV1Parameters getYarn() {
        return yarn;
    }

    public void setYarn(YarnNetworkV1Parameters yarn) {
        this.yarn = yarn;
    }

    public OpenstackNetworkV1Parameters getOpenstack() {
        return openstack;
    }

    public void setOpenstack(OpenstackNetworkV1Parameters openstack) {
        this.openstack = openstack;
    }
}