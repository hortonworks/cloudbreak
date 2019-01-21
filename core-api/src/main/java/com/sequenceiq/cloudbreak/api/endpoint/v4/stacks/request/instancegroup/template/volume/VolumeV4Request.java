package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.AWS_PARAMETERS;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.AZURE_PARAMETERS;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.GCP_PARAMETERS;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.OPENSTACK_PARAMETERS;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.VOLUME_COUNT;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.VOLUME_SIZE;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.VOLUME_TYPE;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription.YARN_PARAMETERS;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.OpenStackInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.YarnInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class VolumeV4Request implements JsonEntity {

    @ApiModelProperty(VOLUME_COUNT)
    private Integer count;

    @ApiModelProperty(VOLUME_SIZE)
    private Integer size;

    @ApiModelProperty(VOLUME_TYPE)
    private String type;

    @ApiModelProperty(AWS_PARAMETERS)
    private AwsInstanceTemplateParametersV4 aws;

    @ApiModelProperty(GCP_PARAMETERS)
    private GcpInstanceTemplateParametersV4 gcp;

    @ApiModelProperty(AZURE_PARAMETERS)
    private AzureInstanceTemplateParametersV4 azure;

    @ApiModelProperty(OPENSTACK_PARAMETERS)
    private OpenStackInstanceTemplateParametersV4 openStack;

    @ApiModelProperty(YARN_PARAMETERS)
    private YarnInstanceTemplateParametersV4 yarn;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public AwsInstanceTemplateParametersV4 getAws() {
        return aws;
    }

    public void setAws(AwsInstanceTemplateParametersV4 aws) {
        this.aws = aws;
    }

    public GcpInstanceTemplateParametersV4 getGcp() {
        return gcp;
    }

    public void setGcp(GcpInstanceTemplateParametersV4 gcp) {
        this.gcp = gcp;
    }

    public AzureInstanceTemplateParametersV4 getAzure() {
        return azure;
    }

    public void setAzure(AzureInstanceTemplateParametersV4 azure) {
        this.azure = azure;
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
