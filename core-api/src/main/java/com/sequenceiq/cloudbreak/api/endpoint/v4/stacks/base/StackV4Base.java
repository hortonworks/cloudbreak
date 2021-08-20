package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import javax.validation.constraints.NotNull;

import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AwsStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.GcpStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.MockStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.OpenStackStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.YarnStackV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.validation.ValidStackNameFormat;
import com.sequenceiq.cloudbreak.validation.ValidStackNameLength;

import io.swagger.annotations.ApiModelProperty;

public abstract class StackV4Base extends ProviderParametersBase implements JsonEntity {

    @ValidStackNameFormat
    @ValidStackNameLength
    @NotNull
    @ApiModelProperty(value = StackModelDescription.STACK_NAME, required = true)
    private String name;

    @ApiModelProperty(value = StackModelDescription.STACK_DISPLAYNAME)
    private String displayName;

    @ApiModelProperty(StackModelDescription.AWS_PARAMETERS)
    private AwsStackV4Parameters aws;

    @ApiModelProperty(StackModelDescription.GCP_PARAMETERS)
    private GcpStackV4Parameters gcp;

    @ApiModelProperty(StackModelDescription.AZURE_PARAMETERS)
    private AzureStackV4Parameters azure;

    @ApiModelProperty(StackModelDescription.OPENSTACK_PARAMETERS)
    private OpenStackStackV4Parameters openstack;

    @ApiModelProperty(StackModelDescription.OPENSTACK_PARAMETERS)
    private YarnStackV4Parameters yarn;

    @ApiModelProperty(hidden = false)
    private MockStackV4Parameters mock;

    @ApiModelProperty
    private Long timeToLive;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public AwsStackV4Parameters createAws() {
        if (aws == null) {
            aws = new AwsStackV4Parameters();
        }
        return aws;
    }

    public void setAws(AwsStackV4Parameters aws) {
        this.aws = aws;
    }

    public GcpStackV4Parameters createGcp() {
        if (gcp == null) {
            gcp = new GcpStackV4Parameters();
        }
        return gcp;
    }

    public void setGcp(GcpStackV4Parameters gcp) {
        this.gcp = gcp;
    }

    public AzureStackV4Parameters createAzure() {
        if (azure == null) {
            azure = new AzureStackV4Parameters();
        }
        return azure;
    }

    public void setAzure(AzureStackV4Parameters azure) {
        this.azure = azure;
    }

    public OpenStackStackV4Parameters createOpenstack() {
        if (openstack == null) {
            openstack = new OpenStackStackV4Parameters();
        }
        return openstack;
    }

    public void setOpenstack(OpenStackStackV4Parameters openstack) {
        this.openstack = openstack;
    }

    @Override
    public YarnStackV4Parameters createYarn() {
        if (yarn == null) {
            yarn = new YarnStackV4Parameters();
        }
        return yarn;
    }

    public void setYarn(YarnStackV4Parameters yarn) {
        this.yarn = yarn;
    }

    @Override
    public MockStackV4Parameters createMock() {
        if (mock == null) {
            mock = new MockStackV4Parameters();
        }
        return mock;
    }

    public AwsStackV4Parameters getAws() {
        return aws;
    }

    public GcpStackV4Parameters getGcp() {
        return gcp;
    }

    public AzureStackV4Parameters getAzure() {
        return azure;
    }

    public OpenStackStackV4Parameters getOpenstack() {
        return openstack;
    }

    public YarnStackV4Parameters getYarn() {
        return yarn;
    }

    public MockStackV4Parameters getMock() {
        return mock;
    }

    public void setMock(MockStackV4Parameters mock) {
        this.mock = mock;
    }

    public Long getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(Long timeToLive) {
        this.timeToLive = timeToLive;
    }

    @Override
    public String toString() {
        return "StackV4Base{" +
                "name='" + name + '\'' +
                "display name ='" + displayName + '\'' +
                ", aws=" + aws +
                ", gcp=" + gcp +
                ", azure=" + azure +
                ", openstack=" + openstack +
                ", yarn=" + yarn +
                ", mock=" + mock +
                ", timeToLive=" + timeToLive +
                '}';
    }
}
