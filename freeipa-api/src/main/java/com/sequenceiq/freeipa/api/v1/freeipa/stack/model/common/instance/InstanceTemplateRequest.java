package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("InstanceTemplateV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceTemplateRequest extends InstanceTemplateBase {

    private Set<VolumeRequest> attachedVolumes;

    @Valid
    @ApiModelProperty(FreeIpaModelDescriptions.AWS_PARAMETERS)
    private AwsInstanceTemplateParameters aws;

    public Set<VolumeRequest> getAttachedVolumes() {
        return attachedVolumes;
    }

    public void setAttachedVolumes(Set<VolumeRequest> attachedVolumes) {
        this.attachedVolumes = attachedVolumes;
    }

    public AwsInstanceTemplateParameters getAws() {
        return aws;
    }

    public void setAws(AwsInstanceTemplateParameters aws) {
        this.aws = aws;
    }
}
