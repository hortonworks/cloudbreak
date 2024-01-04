package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import java.util.Set;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateParameters;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "InstanceTemplateV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceTemplateRequest extends InstanceTemplateBase {

    private Set<VolumeRequest> attachedVolumes;

    private VolumeRequest rootVolume;

    @Valid
    @Schema(description = FreeIpaModelDescriptions.AWS_PARAMETERS)
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

    public VolumeRequest getRootVolume() {
        return rootVolume;
    }

    public void setRootVolume(VolumeRequest rootVolume) {
        this.rootVolume = rootVolume;
    }

    @Override
    public String toString() {
        return "InstanceTemplateRequest{" +
                "attachedVolumes=" + attachedVolumes +
                ", rootVolume=" + rootVolume +
                ", aws=" + aws +
                "} " + super.toString();
    }
}
