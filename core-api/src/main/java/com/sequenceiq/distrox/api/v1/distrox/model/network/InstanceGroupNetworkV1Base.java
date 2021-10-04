package com.sequenceiq.distrox.api.v1.distrox.model.network;

import java.io.Serializable;

import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.NetworkModelDescription;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.InstanceGroupAwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.azure.InstanceGroupAzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.gcp.InstanceGroupGcpNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.mock.InstanceGroupMockNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.openstack.InstanceGroupOpenstackNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.yarn.InstanceGroupYarnNetworkV1Parameters;

import io.swagger.annotations.ApiModelProperty;

public class InstanceGroupNetworkV1Base extends ProviderParametersBase implements Serializable {

    @ApiModelProperty(NetworkModelDescription.AWS_PARAMETERS)
    private InstanceGroupAwsNetworkV1Parameters aws;

    @ApiModelProperty(NetworkModelDescription.AZURE_PARAMETERS)
    private InstanceGroupAzureNetworkV1Parameters azure;

    @ApiModelProperty(NetworkModelDescription.MOCK_PARAMETERS)
    private InstanceGroupMockNetworkV1Parameters mock;

    @ApiModelProperty(NetworkModelDescription.GCP_PARAMETERS)
    private InstanceGroupGcpNetworkV1Parameters gcp;

    @ApiModelProperty(NetworkModelDescription.YARN_PARAMETERS)
    private InstanceGroupYarnNetworkV1Parameters yarn;

    @ApiModelProperty(NetworkModelDescription.OPENSTACK_PARAMETERS_DEPRECATED)
    @Deprecated
    private InstanceGroupOpenstackNetworkV1Parameters openstack;

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

    public InstanceGroupGcpNetworkV1Parameters getGcp() {
        return gcp;
    }

    public void setGcp(InstanceGroupGcpNetworkV1Parameters gcp) {
        this.gcp = gcp;
    }

    public InstanceGroupYarnNetworkV1Parameters getYarn() {
        return yarn;
    }

    public void setYarn(InstanceGroupYarnNetworkV1Parameters yarn) {
        this.yarn = yarn;
    }

    @Override
    public Mappable createAws() {
        if (aws == null) {
            aws = new InstanceGroupAwsNetworkV1Parameters();
        }
        return aws;
    }

    @Override
    public Mappable createGcp() {
        if (gcp == null) {
            gcp = new InstanceGroupGcpNetworkV1Parameters();
        }
        return gcp;
    }

    @Override
    public Mappable createAzure() {
        if (azure == null) {
            azure = new InstanceGroupAzureNetworkV1Parameters();
        }
        return azure;
    }

    @Override
    public Mappable createYarn() {
        if (yarn == null) {
            yarn = new InstanceGroupYarnNetworkV1Parameters();
        }
        return yarn;
    }

    @Override
    public Mappable createMock() {
        if (mock == null) {
            mock = new InstanceGroupMockNetworkV1Parameters();
        }
        return mock;
    }
}