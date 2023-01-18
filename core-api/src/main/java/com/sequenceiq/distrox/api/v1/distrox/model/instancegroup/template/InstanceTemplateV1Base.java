package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template;

import java.io.Serializable;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;
import com.sequenceiq.distrox.api.v1.distrox.model.CloudPlatformProvider;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class InstanceTemplateV1Base implements Serializable, CloudPlatformProvider {

    @Valid
    @Schema(description = TemplateModelDescription.AWS_PARAMETERS)
    private AwsInstanceTemplateV1Parameters aws;

    @Schema(description = TemplateModelDescription.AZURE_PARAMETERS)
    private AzureInstanceTemplateV1Parameters azure;

    @Schema(description = TemplateModelDescription.GCP_PARAMETERS)
    private GcpInstanceTemplateV1Parameters gcp;

    @Schema(description = TemplateModelDescription.YARN_PARAMETERS)
    private YarnInstanceTemplateV1Parameters yarn;

    @Schema(description = TemplateModelDescription.INSTANCE_TYPE)
    private String instanceType;

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    @Override
    public AwsInstanceTemplateV1Parameters getAws() {
        return aws;
    }

    public void setAws(AwsInstanceTemplateV1Parameters aws) {
        this.aws = aws;
    }

    public void setAzure(AzureInstanceTemplateV1Parameters azure) {
        this.azure = azure;
    }

    @Override
    public AzureInstanceTemplateV1Parameters getAzure() {
        return azure;
    }

    @Override
    public GcpInstanceTemplateV1Parameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpInstanceTemplateV1Parameters gcp) {
        this.gcp = gcp;
    }

    public YarnInstanceTemplateV1Parameters getYarn() {
        return yarn;
    }

    public void setYarn(YarnInstanceTemplateV1Parameters yarn) {
        this.yarn = yarn;
    }
}
