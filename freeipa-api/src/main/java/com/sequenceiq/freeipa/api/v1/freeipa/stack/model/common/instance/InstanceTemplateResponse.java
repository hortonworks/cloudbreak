package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;

@ApiModel("InstanceTemplateV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceTemplateResponse extends InstanceTemplateBase {

    private Set<VolumeResponse> attachedVolumes;

    public Set<VolumeResponse> getAttachedVolumes() {
        return attachedVolumes;
    }

    public void setAttachedVolumes(Set<VolumeResponse> attachedVolumes) {
        this.attachedVolumes = attachedVolumes;
    }

    @Override
    public String toString() {
        return "InstanceTemplateResponse{" +
                "InstanceTemplateBase=" + super.toString() +
                ", attachedVolumes=" + attachedVolumes +
                '}';
    }
}
