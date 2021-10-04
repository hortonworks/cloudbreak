package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.OpenStackNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.YarnNetworkV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.NetworkModelDescription;
import com.sequenceiq.cloudbreak.validation.SubnetType;
import com.sequenceiq.cloudbreak.validation.ValidSubnet;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class NetworkV4Base extends ProviderParametersBase implements JsonEntity {

    @ApiModelProperty(NetworkModelDescription.SUBNET_CIDR)
    @ValidSubnet(SubnetType.RFC_1918_COMPLIANT_ONLY)
    private String subnetCIDR;

    @ApiModelProperty(NetworkModelDescription.AWS_PARAMETERS)
    private AwsNetworkV4Parameters aws;

    @ApiModelProperty(NetworkModelDescription.GCP_PARAMETERS)
    private GcpNetworkV4Parameters gcp;

    @ApiModelProperty(NetworkModelDescription.AZURE_PARAMETERS)
    private AzureNetworkV4Parameters azure;

    @ApiModelProperty(NetworkModelDescription.OPENSTACK_PARAMETERS_DEPRECATED)
    @Deprecated
    private OpenStackNetworkV4Parameters openstack;

    @ApiModelProperty(hidden = false)
    private MockNetworkV4Parameters mock;

    @ApiModelProperty(hidden = true)
    private YarnNetworkV4Parameters yarn;

    @Override
    public MockNetworkV4Parameters createMock() {
        if (mock == null) {
            mock = new MockNetworkV4Parameters();
        }
        return mock;
    }

    public void setMock(MockNetworkV4Parameters mock) {
        this.mock = mock;
    }

    public String getSubnetCIDR() {
        return subnetCIDR;
    }

    public void setSubnetCIDR(String subnetCIDR) {
        this.subnetCIDR = subnetCIDR;
    }

    public Optional<Boolean> isNoPublicIp() {
        if (azure != null) {
            return Optional.ofNullable(azure.getNoPublicIp());
        } else if (gcp != null) {
            return Optional.ofNullable(gcp.getNoPublicIp());
        } else {
            return Optional.empty();
        }
    }

    public AwsNetworkV4Parameters createAws() {
        if (aws == null) {
            aws = new AwsNetworkV4Parameters();
        }
        return aws;
    }

    public void setAws(AwsNetworkV4Parameters aws) {
        this.aws = aws;
    }

    public GcpNetworkV4Parameters createGcp() {
        if (gcp == null) {
            gcp = new GcpNetworkV4Parameters();
        }
        return gcp;
    }

    public void setGcp(GcpNetworkV4Parameters gcp) {
        this.gcp = gcp;
    }

    public AzureNetworkV4Parameters createAzure() {
        if (azure == null) {
            azure = new AzureNetworkV4Parameters();
        }
        return azure;
    }

    public void setAzure(AzureNetworkV4Parameters azure) {
        this.azure = azure;
    }

    @Override
    public YarnNetworkV4Parameters createYarn() {
        if (yarn == null) {
            yarn = new YarnNetworkV4Parameters();
        }
        return yarn;
    }

    public void setYarn(YarnNetworkV4Parameters yarn) {
        this.yarn = yarn;
    }

    public AwsNetworkV4Parameters getAws() {
        return aws;
    }

    public GcpNetworkV4Parameters getGcp() {
        return gcp;
    }

    public AzureNetworkV4Parameters getAzure() {
        return azure;
    }

    public MockNetworkV4Parameters getMock() {
        return mock;
    }

    public YarnNetworkV4Parameters getYarn() {
        return yarn;
    }
}
