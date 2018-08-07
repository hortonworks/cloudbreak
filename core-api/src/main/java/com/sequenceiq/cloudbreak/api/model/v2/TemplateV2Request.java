package com.sequenceiq.cloudbreak.api.model.v2;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.CustomInstanceType;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.AzureParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.OpenStackParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.YarnParameters;
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
    private AwsParameters awsParameters;

    @ApiModelProperty(TemplateModelDescription.GCP_PARAMETERS)
    private GcpParameters gcpTemlateParameters;

    @ApiModelProperty(TemplateModelDescription.AZURE_PARAMETERS)
    private AzureParameters azureParameters;

    @ApiModelProperty(TemplateModelDescription.OPENSTACK_PARAMETERS)
    private OpenStackParameters openStackParameters;

    @ApiModelProperty(TemplateModelDescription.YARN_PARAMETERS)
    private YarnParameters yarnParameters;

    @ApiModelProperty(TemplateModelDescription.VOLUME_TYPE)
    private String volumeType;

    @ApiModelProperty(TemplateModelDescription.INSTANCE_TYPE)
    private String instanceType;

    @ApiModelProperty(TemplateModelDescription.CUSTOM_INSTANCE_TYPE)
    private CustomInstanceType customInstanceType;

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

    public YarnParameters getYarnParameters() {
        return yarnParameters;
    }

    public void setYarnParameters(YarnParameters yarnParameters) {
        this.yarnParameters = yarnParameters;
    }
}
