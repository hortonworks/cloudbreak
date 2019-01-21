package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class InstanceTemplateV4Request extends InstanceTemplateV4Base {

    private VolumeV4Request rootVolume;

    private VolumeV4Request ephemeralVolume;

    private Set<VolumeV4Request> attachedVolumes;

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
