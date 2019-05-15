package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import java.util.Set;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class InstanceTemplateBase {
    @ApiModelProperty(TemplateModelDescription.INSTANCE_TYPE)
    private String instanceType;

    private Set<VolumeRequest> attachedVolumes;

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public Set<VolumeRequest> getAttachedVolumes() {
        return attachedVolumes;
    }

    public void setAttachedVolumes(Set<VolumeRequest> attachedVolumes) {
        this.attachedVolumes = attachedVolumes;
    }
}
