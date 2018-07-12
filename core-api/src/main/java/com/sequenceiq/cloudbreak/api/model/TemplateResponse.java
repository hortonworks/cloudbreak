package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsTemplateParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.AzureTemplateParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpTemplateParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.OpenStackTemplateParameters;
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
    private AwsTemplateParameters awsTemplateParameters;

    @ApiModelProperty(TemplateModelDescription.GCP_PARAMETERS)
    private GcpTemplateParameters gcpTemlateParameters;

    @ApiModelProperty(TemplateModelDescription.AZURE_PARAMETERS)
    private AzureTemplateParameters azureTemplateParameters;

    @ApiModelProperty(TemplateModelDescription.OPEN_STACK_PARAMETERS)
    private OpenStackTemplateParameters openStackTemplateParameters;

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

    public AwsTemplateParameters getAwsTemplateParameters() {
        return awsTemplateParameters;
    }

    public void setAwsTemplateParameters(AwsTemplateParameters awsTemplateParameters) {
        this.awsTemplateParameters = awsTemplateParameters;
    }

    public GcpTemplateParameters getGcpTemlateParameters() {
        return gcpTemlateParameters;
    }

    public void setGcpTemlateParameters(GcpTemplateParameters gcpTemlateParameters) {
        this.gcpTemlateParameters = gcpTemlateParameters;
    }

    public AzureTemplateParameters getAzureTemplateParameters() {
        return azureTemplateParameters;
    }

    public void setAzureTemplateParameters(AzureTemplateParameters azureTemplateParameters) {
        this.azureTemplateParameters = azureTemplateParameters;
    }

    public OpenStackTemplateParameters getOpenStackTemplateParameters() {
        return openStackTemplateParameters;
    }

    public void setOpenStackTemplateParameters(OpenStackTemplateParameters openStackTemplateParameters) {
        this.openStackTemplateParameters = openStackTemplateParameters;
    }
}
