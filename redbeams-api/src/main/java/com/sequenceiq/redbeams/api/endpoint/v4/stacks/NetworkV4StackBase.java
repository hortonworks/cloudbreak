package com.sequenceiq.redbeams.api.endpoint.v4.stacks;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsNetworkV4Parameters;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureNetworkV4Parameters;
import com.sequenceiq.redbeams.doc.ModelDescriptions.NetworkModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

public class NetworkV4StackBase extends ProviderParametersBase {

    @ApiModelProperty(NetworkModelDescriptions.AWS_PARAMETERS)
    private AwsNetworkV4Parameters aws;

    @ApiModelProperty(NetworkModelDescriptions.AZURE_PARAMETERS)
    private AzureNetworkV4Parameters azure;

    @ApiModelProperty(NetworkModelDescriptions.GCP_PARAMETERS)
    private GcpNetworkV4Parameters gcp;

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
        if (gcp == null) {
            gcp = new GcpNetworkV4Parameters();
        }
        return gcp;
    }

    @Override
    public Mappable createAzure() {
        if (azure == null) {
            azure = new AzureNetworkV4Parameters();
        }

        return azure;
    }

    public void setAzure(AzureNetworkV4Parameters azure) {
        this.azure = azure;
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

    public AzureNetworkV4Parameters getAzure() {
        return azure;
    }

    public GcpNetworkV4Parameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpNetworkV4Parameters gcp) {
        this.gcp = gcp;
    }

    @Override
    public String toString() {
        return "NetworkV4StackBase{" +
                "aws=" + aws +
                ", azure=" + azure +
                ", gcp=" + gcp +
                '}';
    }
}
