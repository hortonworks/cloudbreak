package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network;

import java.util.List;

import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.NetworkModelDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class NetworkBase extends ProviderParametersBase {
    @ApiModelProperty(NetworkModelDescription.OUTBOUND_INTERNET_TRAFFIC)
    private OutboundInternetTraffic outboundInternetTraffic = OutboundInternetTraffic.ENABLED;

    @ApiModelProperty(NetworkModelDescription.NETWORK_CIDRS)
    private List<String> networkCidrs;

    @ApiModelProperty(NetworkModelDescription.AWS_PARAMETERS)
    private AwsNetworkParameters aws;

    @ApiModelProperty(NetworkModelDescription.AZURE_PARAMETERS)
    private AzureNetworkParameters azure;

    @ApiModelProperty(NetworkModelDescription.GCP_PARAMETERS)
    private GcpNetworkParameters gcp;

    @ApiModelProperty(NetworkModelDescription.OPENSTACK_PARAMETERS_DEPRECATED)
    @Deprecated
    private OpenStackNetworkParameters openstack;

    @ApiModelProperty(hidden = false)
    private MockNetworkParameters mock;

    @ApiModelProperty(hidden = true)
    private YarnNetworkParameters yarn;

    public OutboundInternetTraffic getOutboundInternetTraffic() {
        return outboundInternetTraffic;
    }

    public void setOutboundInternetTraffic(OutboundInternetTraffic outboundInternetTraffic) {
        this.outboundInternetTraffic = outboundInternetTraffic;
    }

    public List<String> getNetworkCidrs() {
        return networkCidrs;
    }

    public void setNetworkCidrs(List<String> networkCidrs) {
        this.networkCidrs = networkCidrs;
    }

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

    public void setMock(MockNetworkParameters mock) {
        this.mock = mock;
    }

    public void setYarn(YarnNetworkParameters yarn) {
        this.yarn = yarn;
    }

    public MockNetworkParameters getMock() {
        return mock;
    }

    public YarnNetworkParameters getYarn() {
        return yarn;
    }

    public GcpNetworkParameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpNetworkParameters gcp) {
        this.gcp = gcp;
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
        return "NetworkBase{" +
                "aws=" + aws +
                ", azure=" + azure +
                ", gcp=" + gcp +
                ", networkCidrs=" + networkCidrs +
                ", mock=" + mock +
                ", yarn=" + yarn +
                '}';
    }
}
