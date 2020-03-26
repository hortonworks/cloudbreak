package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.MockInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.OpenStackInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.YarnInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class InstanceTemplateV4Base extends ProviderParametersBase implements JsonEntity {

    @ApiModelProperty(TemplateModelDescription.AWS_PARAMETERS)
    private AwsInstanceTemplateV4Parameters aws;

    @ApiModelProperty(TemplateModelDescription.AZURE_PARAMETERS)
    private AzureInstanceTemplateV4Parameters azure;

    @ApiModelProperty(TemplateModelDescription.GCP_PARAMETERS)
    private GcpInstanceTemplateV4Parameters gcp;

    @ApiModelProperty(TemplateModelDescription.OPENSTACK_PARAMETERS)
    private OpenStackInstanceTemplateV4Parameters openstack;

    @ApiModelProperty(TemplateModelDescription.YARN_PARAMETERS)
    private YarnInstanceTemplateV4Parameters yarn;

    @ApiModelProperty(TemplateModelDescription.YARN_PARAMETERS)
    private MockInstanceTemplateV4Parameters mock;

    @ApiModelProperty(TemplateModelDescription.INSTANCE_TYPE)
    private String instanceType;

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    @Override
    public AwsInstanceTemplateV4Parameters createAws() {
        if (aws == null) {
            aws = new AwsInstanceTemplateV4Parameters();
        }
        return aws;
    }

    public AwsInstanceTemplateV4Parameters getAws() {
        return aws;
    }

    public void setAws(AwsInstanceTemplateV4Parameters aws) {
        this.aws = aws;
    }

    @Override
    public AzureInstanceTemplateV4Parameters createAzure() {
        if (azure == null) {
            azure = new AzureInstanceTemplateV4Parameters();
        }
        return azure;
    }

    public void setAzure(AzureInstanceTemplateV4Parameters azure) {
        this.azure = azure;
    }

    @Override
    public GcpInstanceTemplateV4Parameters createGcp() {
        if (gcp == null) {
            gcp = new GcpInstanceTemplateV4Parameters();
        }
        return gcp;
    }

    public void setGcp(GcpInstanceTemplateV4Parameters gcp) {
        this.gcp = gcp;
    }

    @Override
    public OpenStackInstanceTemplateV4Parameters createOpenstack() {
        if (openstack == null) {
            openstack = new OpenStackInstanceTemplateV4Parameters();
        }
        return openstack;
    }

    public void setOpenstack(OpenStackInstanceTemplateV4Parameters openstack) {
        this.openstack = openstack;
    }

    @Override
    public YarnInstanceTemplateV4Parameters createYarn() {
        if (yarn == null) {
            yarn = new YarnInstanceTemplateV4Parameters();
        }
        return yarn;
    }

    public void setYarn(YarnInstanceTemplateV4Parameters yarn) {
        this.yarn = yarn;
    }

    @Override
    public MockInstanceTemplateV4Parameters createMock() {
        if (mock == null) {
            mock = new MockInstanceTemplateV4Parameters();
        }
        return mock;
    }

    public void setMock(MockInstanceTemplateV4Parameters mock) {
        this.mock = mock;
    }

    public AzureInstanceTemplateV4Parameters getAzure() {
        return azure;
    }

    public GcpInstanceTemplateV4Parameters getGcp() {
        return gcp;
    }

    public OpenStackInstanceTemplateV4Parameters getOpenstack() {
        return openstack;
    }

    public YarnInstanceTemplateV4Parameters getYarn() {
        return yarn;
    }

    public MockInstanceTemplateV4Parameters getMock() {
        return mock;
    }
}
