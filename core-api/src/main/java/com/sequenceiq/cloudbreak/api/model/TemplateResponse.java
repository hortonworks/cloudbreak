package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.AzureParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.OpenStackParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.YarnParameters;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class TemplateResponse extends TemplateBase {

    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(ModelDescriptions.PUBLIC_IN_ACCOUNT)
    private boolean publicInAccount;

    @ApiModelProperty(value = TemplateModelDescription.VOLUME_COUNT, required = true)
    private Integer volumeCount;

    @ApiModelProperty(value = TemplateModelDescription.VOLUME_SIZE, required = true)
    private Integer volumeSize;

    @ApiModelProperty(TemplateModelDescription.AWS_PARAMETERS)
    private AwsParameters awsParameters;

    @ApiModelProperty(TemplateModelDescription.GCP_PARAMETERS)
    private GcpParameters gcpTemlateParameters;

    @ApiModelProperty(TemplateModelDescription.AZURE_PARAMETERS)
    private AzureParameters azureParameters;

    @ApiModelProperty(TemplateModelDescription.OPENSTACK_PARAMETERS)
    private OpenStackParameters openStackParameters;

    @ApiModelProperty(TemplateModelDescription.YARN_PARAMETERS)
    private YarnParameters yarnParameters;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("public")
    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public Integer getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(Integer volumeSize) {
        this.volumeSize = volumeSize;
    }

    public Integer getVolumeCount() {
        return volumeCount;
    }

    public void setVolumeCount(Integer volumeCount) {
        this.volumeCount = volumeCount;
    }

    public AwsParameters getAwsParameters() {
        return awsParameters;
    }

    public void setAwsParameters(AwsParameters awsParameters) {
        this.awsParameters = awsParameters;
    }

    public GcpParameters getGcpTemlateParameters() {
        return gcpTemlateParameters;
    }

    public void setGcpTemlateParameters(GcpParameters gcpTemlateParameters) {
        this.gcpTemlateParameters = gcpTemlateParameters;
    }

    public AzureParameters getAzureParameters() {
        return azureParameters;
    }

    public void setAzureParameters(AzureParameters azureParameters) {
        this.azureParameters = azureParameters;
    }

    public OpenStackParameters getOpenStackParameters() {
        return openStackParameters;
    }

    public void setOpenStackParameters(OpenStackParameters openStackParameters) {
        this.openStackParameters = openStackParameters;
    }

    public YarnParameters getYarnParameters() {
        return yarnParameters;
    }

    public void setYarnParameters(YarnParameters yarnParameters) {
        this.yarnParameters = yarnParameters;
    }
}
