package com.sequenceiq.redbeams.api.endpoint.v4.stacks;

import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.redbeams.doc.ModelDescriptions.NetworkModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class NetworkV4Base extends ProviderParametersBase {

    @ApiModelProperty(NetworkModelDescription.AWS_PARAMETERS)
    private AwsNetworkV4Parameters aws;

    // @ApiModelProperty(NetworkModelDescription.GCP_PARAMETERS)
    // private GcpNetworkV4Parameters gcp;

    // @ApiModelProperty(NetworkModelDescription.AZURE_PARAMETERS)
    // private AzureNetworkV4Parameters azure;

    // @ApiModelProperty(NetworkModelDescription.OPEN_STACK_PARAMETERS)
    // private OpenStackNetworkV4Parameters openstack;

    // @ApiModelProperty(hidden = true)
    // private MockNetworkV4Parameters mock;

    // @ApiModelProperty(hidden = true)
    // private YarnNetworkV4Parameters yarn;

    @Override
    public AwsNetworkV4Parameters createAws() {
        if (aws == null) {
            aws = new AwsNetworkV4Parameters();
        }
        return aws;
    }

    public void setAws(AwsNetworkV4Parameters aws) {
        this.aws = aws;
    }

    @Override
    public Mappable createGcp() {
        return null;
    }

    @Override
    public Mappable createAzure() {
        return null;
    }

    @Override
    public Mappable createOpenstack() {
        return null;
    }

    @Override
    public Mappable createYarn() {
        return null;
    }

    @Override
    public Mappable createMock() {
        return null;
    }

    public AwsNetworkV4Parameters getAws() {
        return aws;
    }

}
