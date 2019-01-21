package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.OpenStackNetworkParametersV4;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.NetworkModelDescription;
import com.sequenceiq.cloudbreak.validation.ValidSubnet;

import io.swagger.annotations.ApiModelProperty;

public class NetworkV4Base extends ProviderParametersBase implements JsonEntity {

    @ApiModelProperty(NetworkModelDescription.SUBNET_CIDR)
    @ValidSubnet
    private String subnetCIDR;

    @ApiModelProperty(NetworkModelDescription.AWS_PARAMETERS)
    private AwsNetworkParametersV4 aws;

    @ApiModelProperty(NetworkModelDescription.GCP_PARAMETERS)
    private GcpNetworkParametersV4 gcp;

    @ApiModelProperty(NetworkModelDescription.AZURE_PARAMETERS)
    private AzureNetworkParametersV4 azure;

    @ApiModelProperty(NetworkModelDescription.OPEN_STACK_PARAMETERS)
    private OpenStackNetworkParametersV4 openstack;

    public String getSubnetCIDR() {
        return subnetCIDR;
    }

    public void setSubnetCIDR(String subnetCIDR) {
        this.subnetCIDR = subnetCIDR;
    }

    public AwsNetworkParametersV4 getAws() {
        return aws;
    }

    public void setAws(AwsNetworkParametersV4 aws) {
        this.aws = aws;
    }

    public GcpNetworkParametersV4 getGcp() {
        return gcp;
    }

    public void setGcp(GcpNetworkParametersV4 gcp) {
        this.gcp = gcp;
    }

    public AzureNetworkParametersV4 getAzure() {
        return azure;
    }

    public void setAzure(AzureNetworkParametersV4 azure) {
        this.azure = azure;
    }

    public OpenStackNetworkParametersV4 getOpenstack() {
        return openstack;
    }

    public void setOpenstack(OpenStackNetworkParametersV4 openstack) {
        this.openstack = openstack;
    }
}