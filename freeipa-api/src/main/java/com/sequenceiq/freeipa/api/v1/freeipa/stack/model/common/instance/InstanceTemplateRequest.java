package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;

@ApiModel("InstanceTemplateV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceTemplateRequest extends InstanceTemplateBase {

    private Set<VolumeRequest> attachedVolumes;

    public Set<VolumeRequest> getAttachedVolumes() {
        return attachedVolumes;
    }

    public void setAttachedVolumes(Set<VolumeRequest> attachedVolumes) {
        this.attachedVolumes = attachedVolumes;
    }
}
