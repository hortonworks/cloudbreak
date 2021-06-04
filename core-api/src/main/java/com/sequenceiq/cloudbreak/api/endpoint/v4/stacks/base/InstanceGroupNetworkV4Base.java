package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.aws.InstanceGroupAwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.azure.InstanceGroupAzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.gcp.InstanceGroupGcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.mock.InstanceGroupMockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.openstack.InstanceGroupOpenstackNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.yarn.InstanceGroupYarnNetworkV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.NetworkModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class InstanceGroupNetworkV4Base extends ProviderParametersBase implements JsonEntity {

    @ApiModelProperty(NetworkModelDescription.AWS_PARAMETERS)
    private InstanceGroupAwsNetworkV4Parameters aws;

    @ApiModelProperty(NetworkModelDescription.GCP_PARAMETERS)
    private InstanceGroupGcpNetworkV4Parameters gcp;

    @ApiModelProperty(NetworkModelDescription.AZURE_PARAMETERS)
    private InstanceGroupAzureNetworkV4Parameters azure;

    @ApiModelProperty(NetworkModelDescription.OPEN_STACK_PARAMETERS)
    private InstanceGroupOpenstackNetworkV4Parameters openstack;

    @ApiModelProperty(hidden = true)
    private InstanceGroupMockNetworkV4Parameters mock;

    @ApiModelProperty(hidden = true)
    private InstanceGroupYarnNetworkV4Parameters yarn;

    @Override
    public InstanceGroupMockNetworkV4Parameters createMock() {
        if (mock == null) {
            mock = new InstanceGroupMockNetworkV4Parameters();
        }
        return mock;
    }

    public void setMock(InstanceGroupMockNetworkV4Parameters mock) {
        this.mock = mock;
    }

    public InstanceGroupAwsNetworkV4Parameters createAws() {
        if (aws == null) {
            aws = new InstanceGroupAwsNetworkV4Parameters();
        }
        return aws;
    }

    public void setAws(InstanceGroupAwsNetworkV4Parameters aws) {
        this.aws = aws;
    }

    public InstanceGroupGcpNetworkV4Parameters createGcp() {
        if (gcp == null) {
            gcp = new InstanceGroupGcpNetworkV4Parameters();
        }
        return gcp;
    }

    public void setGcp(InstanceGroupGcpNetworkV4Parameters gcp) {
        this.gcp = gcp;
    }

    public InstanceGroupAzureNetworkV4Parameters createAzure() {
        if (azure == null) {
            azure = new InstanceGroupAzureNetworkV4Parameters();
        }
        return azure;
    }

    public void setAzure(InstanceGroupAzureNetworkV4Parameters azure) {
        this.azure = azure;
    }

    public InstanceGroupOpenstackNetworkV4Parameters createOpenstack() {
        if (openstack == null) {
            openstack = new InstanceGroupOpenstackNetworkV4Parameters();
        }
        return openstack;
    }

    public void setOpenstack(InstanceGroupOpenstackNetworkV4Parameters openstack) {
        this.openstack = openstack;
    }

    @Override
    public InstanceGroupYarnNetworkV4Parameters createYarn() {
        if (yarn == null) {
            yarn = new InstanceGroupYarnNetworkV4Parameters();
        }
        return yarn;
    }

    public void setYarn(InstanceGroupYarnNetworkV4Parameters yarn) {
        this.yarn = yarn;
    }

    public InstanceGroupAwsNetworkV4Parameters getAws() {
        return aws;
    }

    public InstanceGroupGcpNetworkV4Parameters getGcp() {
        return gcp;
    }

    public InstanceGroupAzureNetworkV4Parameters getAzure() {
        return azure;
    }

    public InstanceGroupOpenstackNetworkV4Parameters getOpenstack() {
        return openstack;
    }

    public InstanceGroupMockNetworkV4Parameters getMock() {
        return mock;
    }

    public InstanceGroupYarnNetworkV4Parameters getYarn() {
        return yarn;
    }
}
