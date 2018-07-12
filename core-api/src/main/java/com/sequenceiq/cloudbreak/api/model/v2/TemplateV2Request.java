package com.sequenceiq.cloudbreak.api.model.v2;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.CustomInstanceType;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsTemplateParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.AzureTemplateParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpTemplateParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.OpenStackTemplateParameters;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class TemplateV2Request implements JsonEntity {

    @ApiModelProperty(TemplateModelDescription.VOLUME_COUNT)
    private Integer volumeCount;

    @ApiModelProperty(TemplateModelDescription.VOLUME_SIZE)
    private Integer volumeSize;

    @ApiModelProperty(TemplateModelDescription.ROOT_VOLUME_SIZE)
    private Integer rootVolumeSize;

    @ApiModelProperty(TemplateModelDescription.PARAMETERS)
    private Map<String, Object> parameters = new HashMap<>();

    @ApiModelProperty(TemplateModelDescription.AWS_PARAMETERS)
    private AwsTemplateParameters awsTemplateParameters;

    @ApiModelProperty(TemplateModelDescription.GCP_PARAMETERS)
    private GcpTemplateParameters gcpTemlateParameters;

    @ApiModelProperty(TemplateModelDescription.AZURE_PARAMETERS)
    private AzureTemplateParameters azureTemplateParameters;

    @ApiModelProperty(TemplateModelDescription.OPEN_STACK_PARAMETERS)
    private OpenStackTemplateParameters openStackTemplateParameters;

    @ApiModelProperty(TemplateModelDescription.VOLUME_TYPE)
    private String volumeType;

    @ApiModelProperty(TemplateModelDescription.INSTANCE_TYPE)
    private String instanceType;

    @ApiModelProperty(TemplateModelDescription.CUSTOM_INSTANCE_TYPE)
    private CustomInstanceType customInstanceType;

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

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public CustomInstanceType getCustomInstanceType() {
        return customInstanceType;
    }

    public void setCustomInstanceType(CustomInstanceType customInstanceType) {
        this.customInstanceType = customInstanceType;
    }

    public Integer getRootVolumeSize() {
        return rootVolumeSize;
    }

    public void setRootVolumeSize(Integer rootVolumeSize) {
        this.rootVolumeSize = rootVolumeSize;
    }
}
