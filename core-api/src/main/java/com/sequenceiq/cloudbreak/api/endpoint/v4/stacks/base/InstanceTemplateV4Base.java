package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.OpenStackInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.YarnInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class InstanceTemplateV4Base extends ProviderParametersBase implements JsonEntity {

    @ApiModelProperty(TemplateModelDescription.AWS_PARAMETERS)
    private AwsInstanceTemplateParametersV4 aws;

    @ApiModelProperty(TemplateModelDescription.AZURE_PARAMETERS)
    private AzureInstanceTemplateParametersV4 azure;

    @ApiModelProperty(TemplateModelDescription.GCP_PARAMETERS)
    private GcpInstanceTemplateParametersV4 gcp;

    @ApiModelProperty(TemplateModelDescription.OPENSTACK_PARAMETERS)
    private OpenStackInstanceTemplateParametersV4 openStack;

    @ApiModelProperty(TemplateModelDescription.YARN_PARAMETERS)
    private YarnInstanceTemplateParametersV4 yarn;

    @ApiModelProperty(TemplateModelDescription.INSTANCE_TYPE)
    private String instanceType;

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public AwsInstanceTemplateParametersV4 getAws() {
        return aws;
    }

    public void setAws(AwsInstanceTemplateParametersV4 aws) {
        this.aws = aws;
    }

    public AzureInstanceTemplateParametersV4 getAzure() {
        return azure;
    }

    public void setAzure(AzureInstanceTemplateParametersV4 azure) {
        this.azure = azure;
    }

    public GcpInstanceTemplateParametersV4 getGcp() {
        return gcp;
    }

    public void setGcp(GcpInstanceTemplateParametersV4 gcp) {
        this.gcp = gcp;
    }

    public OpenStackInstanceTemplateParametersV4 getOpenStack() {
        return openStack;
    }

    public void setOpenStack(OpenStackInstanceTemplateParametersV4 openStack) {
        this.openStack = openStack;
    }

    public YarnInstanceTemplateParametersV4 getYarn() {
        return yarn;
    }

    public void setYarn(YarnInstanceTemplateParametersV4 yarn) {
        this.yarn = yarn;
    }
}
