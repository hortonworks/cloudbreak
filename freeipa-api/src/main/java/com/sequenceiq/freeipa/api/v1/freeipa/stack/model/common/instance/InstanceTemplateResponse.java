package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "InstanceTemplateV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceTemplateResponse extends InstanceTemplateBase {

    private Set<VolumeResponse> attachedVolumes;

    private Map<String, Object> attributes = new HashMap<>();

    public Set<VolumeResponse> getAttachedVolumes() {
        return attachedVolumes;
    }

    public void setAttachedVolumes(Set<VolumeResponse> attachedVolumes) {
        this.attachedVolumes = attachedVolumes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "InstanceTemplateResponse{" +
                "InstanceTemplateBase=" + super.toString() +
                ", attachedVolumes=" + attachedVolumes +
                ", attributes=" + attributes +
                '}';
    }
}
