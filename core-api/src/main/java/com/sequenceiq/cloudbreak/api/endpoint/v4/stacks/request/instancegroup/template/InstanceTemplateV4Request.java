package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.custominstance.CustomInstanceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class InstanceTemplateV4Request extends InstanceTemplateV4Base {

    @ApiModelProperty(TemplateModelDescription.CUSTOM_INSTANCE_TYPE)
    private CustomInstanceV4Request customInstance;

    private VolumeV4Request rootVolume;

    private VolumeV4Request ephemeralVolume;

    private Set<VolumeV4Request> attachedVolumes;

    public CustomInstanceV4Request getCustomInstance() {
        return customInstance;
    }

    public void setCustomInstance(CustomInstanceV4Request customInstance) {
        this.customInstance = customInstance;
    }

    public VolumeV4Request getRootVolume() {
        return rootVolume;
    }

    public void setRootVolume(VolumeV4Request rootVolume) {
        this.rootVolume = rootVolume;
    }

    public VolumeV4Request getEphemeralVolume() {
        return ephemeralVolume;
    }

    public void setEphemeralVolume(VolumeV4Request ephemeralVolume) {
        this.ephemeralVolume = ephemeralVolume;
    }

    public Set<VolumeV4Request> getAttachedVolumes() {
        return attachedVolumes;
    }

    public void setAttachedVolumes(Set<VolumeV4Request> attachedVolumes) {
        this.attachedVolumes = attachedVolumes;
    }
}
