package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network;

import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.NetworkModelDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class NetworkBase extends ProviderParametersBase {
    @ApiModelProperty(NetworkModelDescription.AWS_PARAMETERS)
    private AwsNetworkParameters aws;

    @ApiModelProperty(NetworkModelDescription.AZURE_PARAMETERS)
    private AzureNetworkParameters azure;

    @ApiModelProperty(NetworkModelDescription.GCP_PARAMETERS)
    private GcpNetworkParameters gcp;

    @ApiModelProperty(NetworkModelDescription.OPEN_STACK_PARAMETERS)
    private OpenStackNetworkParameters openstack;

    @ApiModelProperty(hidden = true)
    private MockNetworkParameters mock;

    @ApiModelProperty(hidden = true)
    private YarnNetworkParameters yarn;

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

    public void setGcp(GcpNetworkParameters gcp) {
        this.gcp = gcp;
    }

    public void setOpenstack(OpenStackNetworkParameters openstack) {
        this.openstack = openstack;
    }

    public void setMock(MockNetworkParameters mock) {
        this.mock = mock;
    }

    public void setYarn(YarnNetworkParameters yarn) {
        this.yarn = yarn;
    }

    public GcpNetworkParameters getGcp() {
        return gcp;
    }

    public OpenStackNetworkParameters getOpenstack() {
        return openstack;
    }

    public MockNetworkParameters getMock() {
        return mock;
    }

    public YarnNetworkParameters getYarn() {
        return yarn;
    }

    @Override
    public Mappable createAws() {
        if (aws == null) {
            aws = new AwsNetworkParameters();
        }
        return aws;
    }

    @Override
    public Mappable createGcp() {
        if (gcp == null) {
            gcp = new GcpNetworkParameters();
        }
        return gcp;
    }

    @Override
    public Mappable createAzure() {
        if (azure == null) {
            azure = new AzureNetworkParameters();
        }
        return azure;
    }

    @Override
    public Mappable createOpenstack() {
        if (openstack == null) {
            openstack = new OpenStackNetworkParameters();
        }
        return openstack;
    }

    @Override
    public Mappable createYarn() {
        if (yarn == null) {
            yarn = new YarnNetworkParameters();
        }
        return yarn;
    }

    @Override
    public Mappable createMock() {
        if (mock == null) {
            mock = new MockNetworkParameters();
        }
        return mock;
    }

    @Override
    public String toString() {
        return "NetworkBase{"
                + "aws=" + aws
                + ", azure=" + azure
                + ", gcp=" + gcp
                + ", openstack=" + openstack
                + ", mock=" + mock
                + ", yarn=" + yarn
                + '}';
    }
}
